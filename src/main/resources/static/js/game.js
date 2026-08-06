/**
 * Grand Theft Cheltenham – UK left-hand drive, free within road width
 * Spawn: Cheltenham Promenade (town centre)
 */
const BASE = (() => {
  const p = window.location.pathname;
  if (p.endsWith('/')) return p.slice(0, -1) || '';
  const i = p.lastIndexOf('/');
  return i > 0 ? p.slice(0, i) : '';
})();
const api = (path) => BASE + path;

const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');
const hudFps = document.getElementById('fps');
const hudSpeed = document.getElementById('speed');
const hudHeading = document.getElementById('heading');
const hudPos = document.getElementById('pos');
const roadToast = document.getElementById('roadToast');
const streetNameEl = document.getElementById('streetName');
const titleSplash = document.getElementById('titleSplash');
const startBtn = document.getElementById('startBtn');

function resize() {
  canvas.width = window.innerWidth;
  canvas.height = window.innerHeight;
}
window.addEventListener('resize', resize);
resize();

const keys = { throttle: 0, brake: 0, steer: 0 };
const keyMap = {
  ArrowUp: 'throttle', w: 'throttle', W: 'throttle',
  ArrowDown: 'brake', s: 'brake', S: 'brake',
  ArrowLeft: 'steerLeft', a: 'steerLeft', A: 'steerLeft',
  ArrowRight: 'steerRight', d: 'steerRight', D: 'steerRight'
};
window.addEventListener('keydown', e => {
  const a = keyMap[e.key];
  if (a === 'throttle') keys.throttle = 1;
  if (a === 'brake') keys.brake = 1;
  if (a === 'steerLeft') keys.steer = -1;
  if (a === 'steerRight') keys.steer = 1;
  if (a) e.preventDefault();
});
window.addEventListener('keyup', e => {
  const a = keyMap[e.key];
  if (a === 'throttle') keys.throttle = 0;
  if (a === 'brake') keys.brake = 0;
  if (a === 'steerLeft' || a === 'steerRight') keys.steer = 0;
});

const TOWN_CENTRE = { x: -231311, y: 6781884 };
const car = { x: TOWN_CENTRE.x, y: TOWN_CENTRE.y, heading: 0, speed: 0, steerAngle: 0, onRoad: false };
const PHYS = { L: 2.55, maxSteer: 0.55, steerRate: 2.8, driveForce: 1600, brakeForce: 3800, reverseForce: 1100, mass: 950, drag: 0.36, roll: 0.015, maxSpeed: 48, maxReverse: 12 };
const DT = 1 / 60;

function roadHalfWidth(h) {
  if (h === 'motorway' || h === 'trunk') return 9;
  if (h === 'primary') return 7;
  if (h === 'secondary') return 6;
  if (h === 'tertiary') return 5.5;
  if (h === 'residential' || h === 'unclassified') return 5;
  if (h === 'service' || h === 'living_street') return 3.5;
  return 4.5;
}

function physicsStep() {
  const targetSteer = -keys.steer * PHYS.maxSteer;
  const ds = targetSteer - car.steerAngle;
  const maxStep = PHYS.steerRate * DT;
  car.steerAngle += Math.max(-maxStep, Math.min(maxStep, ds));
  const almostStopped = Math.abs(car.speed) < 1.5;
  let force = 0;
  if (keys.throttle) {
    if (car.speed < -0.5) force = PHYS.brakeForce;
    else force = PHYS.driveForce;
  } else if (keys.brake) {
    if (car.speed > 0.4) force = -PHYS.brakeForce;
    else if (car.speed > -0.2 && almostStopped) force = -PHYS.reverseForce;
    else if (car.speed <= 0) force = -PHYS.reverseForce;
  }
  force -= PHYS.drag * car.speed * Math.abs(car.speed) * 0.5 * 1.15;
  force -= PHYS.roll * PHYS.mass * 9.81 * Math.sign(car.speed || 0);
  car.speed += (force / PHYS.mass) * DT;
  if (car.speed > PHYS.maxSpeed) car.speed = PHYS.maxSpeed;
  if (car.speed < -PHYS.maxReverse) car.speed = -PHYS.maxReverse;
  if (Math.abs(car.speed) < 0.04 && !keys.throttle && !keys.brake) car.speed = 0;
  const spd = Math.abs(car.speed);
  const steerEff = spd < 0.15 ? 0 : (0.35 + 0.65 * Math.min(1, spd / 8));
  if (spd > 0.12) car.heading += (car.speed / PHYS.L) * Math.tan(car.steerAngle) * steerEff * DT;
  const prevX = car.x, prevY = car.y;
  car.x += Math.cos(car.heading) * car.speed * DT;
  car.y += Math.sin(car.heading) * car.speed * DT;
  const blocked = constrainToRoad(prevX, prevY);
  if (blocked) {
    car.speed *= 0.15;
    if (Math.abs(car.speed) < 0.8) car.speed = 0;
  }
}

function closestOnSeg(px, py, ax, ay, bx, by) {
  const abx = bx - ax, aby = by - ay;
  const len2 = abx * abx + aby * aby;
  if (len2 < 1e-6) return { x: ax, y: ay, t: 0, dist: Math.hypot(px - ax, py - ay), tx: 1, ty: 0, len: 0 };
  let t = ((px - ax) * abx + (py - ay) * aby) / len2;
  const tClamped = Math.max(0, Math.min(1, t));
  const x = ax + tClamped * abx, y = ay + tClamped * aby;
  const len = Math.sqrt(len2);
  return { x, y, t: tClamped, tRaw: t, dist: Math.hypot(px - x, py - y), tx: abx / len, ty: aby / len, len };
}

function findNearestRoad(px, py) {
  let best = null;
  for (const road of roadCache) {
    const c = road.coords;
    if (!c || c.length < 2) continue;
    const half = roadHalfWidth(road.highway);
    for (let i = 0; i < c.length - 1; i++) {
      const hit = closestOnSeg(px, py, c[i][0], c[i][1], c[i + 1][0], c[i + 1][1]);
      if (!best || hit.dist < best.dist) {
        best = hit; best.half = half; best.name = road.name; best.highway = road.highway;
      }
    }
  }
  return best;
}

function constrainToRoad(prevX, prevY) {
  if (!roadCache.length) { car.onRoad = false; return false; }
  const hit = findNearestRoad(car.x, car.y);
  if (!hit) { car.onRoad = false; return false; }
  let tx = hit.tx, ty = hit.ty;
  const along = Math.cos(car.heading) * tx + Math.sin(car.heading) * ty;
  if (along < 0) { tx = -tx; ty = -ty; }
  const lx = -ty, ly = tx;
  const lat = (car.x - hit.x) * lx + (car.y - hit.y) * ly;
  const maxLat = hit.half - 0.9;
  let blocked = false;
  if (hit.tRaw !== undefined && (hit.tRaw < -0.02 || hit.tRaw > 1.02)) {
    car.x = hit.x + lx * Math.max(-maxLat, Math.min(maxLat, lat));
    car.y = hit.y + ly * Math.max(-maxLat, Math.min(maxLat, lat));
    blocked = true;
  } else if (Math.abs(lat) > maxLat) {
    const clamped = Math.max(-maxLat, Math.min(maxLat, lat));
    car.x = hit.x + lx * clamped;
    car.y = hit.y + ly * clamped;
    car.speed *= 0.92;
  }
  if (hit.dist > hit.half + 8) { car.x = prevX; car.y = prevY; blocked = true; }
  car.onRoad = hit.dist <= hit.half + 1.5;
  return blocked;
}

function spawnOnTownCentreRoad() {
  car.x = TOWN_CENTRE.x; car.y = TOWN_CENTRE.y; car.speed = 0; car.steerAngle = 0;
  const hit = findNearestRoad(TOWN_CENTRE.x, TOWN_CENTRE.y);
  if (!hit) return false;
  let tx = hit.tx, ty = hit.ty;
  car.heading = Math.atan2(ty, tx);
  const lx = -ty, ly = tx;
  const leftLane = Math.min(2.2, hit.half * 0.45);
  car.x = hit.x + lx * leftLane; car.y = hit.y + ly * leftLane;
  car.onRoad = true;
  if (hit.name) showRoadToast(hit.name); else showRoadToast('Cheltenham town centre');
  return true;
}

function cameraScale() {
  const v = Math.abs(car.speed);
  const t = Math.min(Math.max(v / 40, 0), 1);
  const s = t * t * (3 - 2 * t);
  return 16 + (7 - 16) * s;
}

let roadCache = [], featureCache = [];
let lastFetchPos = { x: 0, y: 0 };
let fetchInFlight = false;

function parseCoords(gj) {
  if (!gj) return null;
  if (typeof gj === 'string') { try { gj = JSON.parse(gj); } catch (e) { return null; } }
  if (gj && gj.type === 'json' && typeof gj.value === 'string') {
    try { gj = JSON.parse(gj.value); } catch (e) { return null; }
  }
  if (!gj || !gj.type) return null;
  if (gj.type === 'LineString' && Array.isArray(gj.coordinates))
    return gj.coordinates.map(function (c) { return [c[0], c[1]]; });
  if (gj.type === 'Polygon' && Array.isArray(gj.coordinates))
    return (gj.coordinates[0] || []).map(function (c) { return [c[0], c[1]]; });
  if (gj.type === 'MultiPolygon' && Array.isArray(gj.coordinates))
    return ((gj.coordinates[0] || [])[0] || []).map(function (c) { return [c[0], c[1]]; });
  return null;
}

async function loadNearby() {
  try {
    const [roadsRes, featRes] = await Promise.all([
      fetch(api('/api/map/roads/nearby?x=' + car.x + '&y=' + car.y + '&radius=1800&limit=900')),
      fetch(api('/api/map/features/nearby?x=' + car.x + '&y=' + car.y + '&radius=1800&limit=500'))
    ]);
    const roadsData = await roadsRes.json();
    const featData = await featRes.json();
    const newRoads = [];
    for (const row of (roadsData.roads || [])) {
      const coords = parseCoords(row.geojson);
      if (coords && coords.length >= 2)
        newRoads.push({ coords: coords, name: row.name, highway: row.highway_type });
    }
    const newFeats = [];
    for (const row of (featData.features || [])) {
      const coords = parseCoords(row.geojson);
      if (coords && coords.length >= 3)
        newFeats.push({ coords: coords, kind: row.kind, name: row.name });
    }
    if (newRoads.length) roadCache = newRoads;
    if (newFeats.length) featureCache = newFeats;
    lastFetchPos = { x: car.x, y: car.y };
  } catch (e) { console.warn('loadNearby failed', e); }
}

async function fetchOsmAround() {
  if (fetchInFlight) return;
  fetchInFlight = true;
  try {
    const r = await fetch(api('/api/map/fetch-around?x=' + car.x + '&y=' + car.y + '&radius=3000'), { method: 'POST' });
    const data = await r.json();
    console.log('OSM fetch-around', data);
    await loadNearby();
    if (data.ok && (data.segmentsWritten || 0) > 0)
      showRoadToast('Map updated (+' + data.segmentsWritten + ' roads)');
  } catch (e) { console.warn('OSM fetch failed', e); }
  finally { fetchInFlight = false; }
}

function maybeRefreshMap() {
  const dist = Math.hypot(car.x - lastFetchPos.x, car.y - lastFetchPos.y);
  if (dist > 450 || roadCache.length === 0) loadNearby();
  if (roadCache.length < 15 && !fetchInFlight) fetchOsmAround();
  else if (dist > 900 && !fetchInFlight) fetchOsmAround();
}

const FEATURE_FILL = {
  building: '#5a5348', park: '#3d6b3d', grass: '#3a6a3a',
  wood: '#2f5530', water: '#2a4a6a', cemetery: '#3a4a3a'
};
function roadMetres(h) { return roadHalfWidth(h) * 2; }

function draw() {
  const w = canvas.width, h = canvas.height;
  const scale = cameraScale();
  ctx.imageSmoothingEnabled = false;
  ctx.fillStyle = '#2d4f2d';
  ctx.fillRect(0, 0, w, h);
  const cx = Math.round(car.x * 4) / 4;
  const cy = Math.round(car.y * 4) / 4;
  const ch = car.heading;
  ctx.save();
  ctx.translate(w / 2, h / 2);
  ctx.scale(scale, -scale);
  ctx.rotate(-ch + Math.PI / 2);
  ctx.translate(-cx, -cy);
  ctx.strokeStyle = 'rgba(0,0,0,0.12)';
  ctx.lineWidth = 0.15;
  const grid = 40;
  const g0x = Math.floor(car.x / grid) * grid;
  const g0y = Math.floor(car.y / grid) * grid;
  for (let i = -30; i <= 30; i++) {
    ctx.beginPath(); ctx.moveTo(g0x + i * grid, g0y - 30 * grid); ctx.lineTo(g0x + i * grid, g0y + 30 * grid); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(g0x - 30 * grid, g0y + i * grid); ctx.lineTo(g0x + 30 * grid, g0y + i * grid); ctx.stroke();
  }
  for (const kind of ['wood', 'grass', 'park', 'cemetery', 'water', 'building']) {
    for (const f of featureCache) {
      if (f.kind !== kind) continue;
      const c = f.coords;
      if (c.length < 3) continue;
      ctx.beginPath();
      ctx.moveTo(c[0][0], c[0][1]);
      for (let i = 1; i < c.length; i++) ctx.lineTo(c[i][0], c[i][1]);
      ctx.closePath();
      ctx.fillStyle = FEATURE_FILL[kind] || '#555';
      ctx.fill();
      if (kind === 'building') { ctx.strokeStyle = 'rgba(0,0,0,0.35)'; ctx.lineWidth = 0.25; ctx.stroke(); }
    }
  }
  for (const road of roadCache) {
    const c = road.coords;
    if (c.length < 2) continue;
    ctx.strokeStyle = '#1a1a1a';
    ctx.lineWidth = roadMetres(road.highway) + 1.8;
    ctx.lineCap = 'round'; ctx.lineJoin = 'round';
    ctx.beginPath(); ctx.moveTo(c[0][0], c[0][1]);
    for (let i = 1; i < c.length; i++) ctx.lineTo(c[i][0], c[i][1]);
    ctx.stroke();
  }
  for (const road of roadCache) {
    const c = road.coords;
    if (c.length < 2) continue;
    ctx.strokeStyle = '#3a3a3a';
    ctx.lineWidth = roadMetres(road.highway);
    ctx.lineCap = 'round'; ctx.lineJoin = 'round';
    ctx.beginPath(); ctx.moveTo(c[0][0], c[0][1]);
    for (let i = 1; i < c.length; i++) ctx.lineTo(c[i][0], c[i][1]);
    ctx.stroke();
  }
  for (const road of roadCache) {
    if (['motorway', 'trunk', 'primary', 'secondary', 'tertiary', 'residential'].indexOf(road.highway) < 0) continue;
    const c = road.coords;
    if (c.length < 2) continue;
    ctx.strokeStyle = 'rgba(220,200,80,0.4)';
    ctx.lineWidth = 0.3;
    ctx.setLineDash([3.5, 4.5]);
    ctx.beginPath(); ctx.moveTo(c[0][0], c[0][1]);
    for (let i = 1; i < c.length; i++) ctx.lineTo(c[i][0], c[i][1]);
    ctx.stroke();
    ctx.setLineDash([]);
  }
  ctx.save();
  ctx.translate(car.x, car.y);
  ctx.rotate(car.heading - Math.PI / 2);
  ctx.fillStyle = '#6B4423';
  ctx.fillRect(-1.15, -2.6, 2.3, 5.2);
  ctx.fillStyle = '#a8d4e8';
  ctx.fillRect(-0.9, 0.7, 1.8, 1.15);
  ctx.fillStyle = '#3d2814';
  ctx.fillRect(-1.15, -2.6, 2.3, 0.35);
  ctx.fillStyle = '#ffffff';
  ctx.beginPath(); ctx.moveTo(0, 2.85); ctx.lineTo(-0.55, 1.85); ctx.lineTo(0.55, 1.85); ctx.closePath(); ctx.fill();
  ctx.restore();
  ctx.restore();
}

let lastRoadName = null, toastTimer = 0;
function setStreetName(name) {
  const label = name || '-';
  if (streetNameEl && streetNameEl.textContent !== label) streetNameEl.textContent = label;
  if (name && name !== lastRoadName) {
    lastRoadName = name;
    roadToast.textContent = name;
    roadToast.classList.add('visible');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { roadToast.classList.remove('visible'); }, 1800);
  }
}
function showRoadToast(name) { setStreetName(name); }
async function pollRoadName() {
  try {
    const r = await fetch(api('/api/map/road-name?x=' + car.x + '&y=' + car.y));
    const data = await r.json();
    if (data.found && data.name) setStreetName(data.name);
    else if (data.found === false) setStreetName('-');
  } catch (_) {}
}

const RENDER_DT = 1 / 40;
let lastTime = performance.now(), physAcc = 0, renderAcc = 0, frames = 0, fpsTimer = 0, running = false;
function loop(now) {
  if (!running) return;
  const frameDt = Math.min((now - lastTime) / 1000, 0.1);
  lastTime = now; physAcc += frameDt; renderAcc += frameDt; fpsTimer += frameDt;
  while (physAcc >= DT) { physicsStep(); physAcc -= DT; }
  if (physAcc > DT * 3) physAcc = 0;
  if (renderAcc >= RENDER_DT) {
    while (renderAcc >= RENDER_DT) renderAcc -= RENDER_DT;
    draw();
    frames++;
    hudSpeed.textContent = Math.round(car.speed * 2.23694);
    hudHeading.textContent = Math.round(((car.heading * 180 / Math.PI) % 360 + 360) % 360);
    hudPos.textContent = car.x.toFixed(0) + ', ' + car.y.toFixed(0);
  }
  if (fpsTimer >= 0.5) { hudFps.textContent = Math.round(frames / fpsTimer); frames = 0; fpsTimer = 0; }
  requestAnimationFrame(loop);
}
setInterval(function () { if (running) pollRoadName(); }, 500);
setInterval(function () { if (running) maybeRefreshMap(); }, 2500);

startBtn.addEventListener('click', async function () {
  titleSplash.style.opacity = '0';
  setTimeout(function () { titleSplash.style.display = 'none'; }, 600);
  car.x = TOWN_CENTRE.x; car.y = TOWN_CENTRE.y; car.speed = 0;
  showRoadToast('Loading Promenade…');
  await loadNearby();
  if (roadCache.length < 5) await fetchOsmAround();
  if (!spawnOnTownCentreRoad()) { await fetchOsmAround(); spawnOnTownCentreRoad(); }
  running = true; lastTime = performance.now(); requestAnimationFrame(loop);
  try {
    const st = await fetch(api('/api/map/status')).then(function (r) { return r.json(); });
    document.getElementById('debugExtra').textContent = 'Brown Astra Mk1 | Town centre | Roads: ' + st.roads;
  } catch (_) {}
});
fetch(api('/api/health')).then(function (r) { return r.json(); }).then(function (h) {
  console.log('GTC health', h);
}).catch(console.warn);
