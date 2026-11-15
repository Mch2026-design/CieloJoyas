'use strict';
(() => {
  const { api, esc, money, busy } = Cielo;
  const $ = s => document.querySelector(s);
  let allProducts = [];
  let selectedIds = new Set();
  let minProducts = 8;

  const msgEl = $('#carousel-message');
  const errEl = $('#carousel-error');
  const speedRange = $('#carousel-speed');
  const speedNum = $('#carousel-speed-num');
  const speedLabel = $('#carousel-speed-label');
  const dirSelect = $('#carousel-direction');
  const countEl = $('#carousel-count');
  const searchInput = $('#carousel-search');
  const grid = $('#carousel-products');

  function syncSpeed(val) {
    const clamped = Math.max(10, Math.min(180, Number(val) || 55));
    speedRange.value = clamped;
    speedNum.value = clamped;
    speedLabel.textContent = clamped + 's';
  }

  function updateCount() {
    const n = selectedIds.size;
    countEl.textContent = `(${n} ${n === 1 ? 'seleccionado' : 'seleccionados'} · mínimo ${minProducts})`;
    countEl.style.color = n < minProducts ? '#9d2b24' : '#1f5126';
  }

  function renderGrid() {
    const q = (searchInput.value || '').trim().toLocaleLowerCase('es');
    const filtered = allProducts.filter(p => !q || `${p.name} ${p.category} ${p.reference || ''}`.toLocaleLowerCase('es').includes(q));
    grid.innerHTML = filtered.map(p => `
      <label class="carousel-item-option">
        <input type="checkbox" value="${p.id}" ${selectedIds.has(p.id) ? 'checked' : ''}>
        <img src="${esc(p.src)}" alt="" width="48" height="58" loading="lazy">
        <span class="carousel-item-info">
          <strong>${esc(p.name)}</strong>
          <small>${esc(p.category)} · ${money(p.price)}</small>
        </span>
      </label>
    `).join('');
    updateCount();
  }

  speedRange.addEventListener('input', () => syncSpeed(speedRange.value));
  speedNum.addEventListener('input', () => syncSpeed(speedNum.value));
  searchInput.addEventListener('input', renderGrid);

  grid.addEventListener('change', e => {
    const cb = e.target.closest('input[type="checkbox"]');
    if (!cb) return;
    const id = Number(cb.value);
    if (cb.checked) selectedIds.add(id);
    else selectedIds.delete(id);
    updateCount();
  });

  $('#carousel-pick-20').addEventListener('click', () => {
    selectedIds = new Set(allProducts.slice(0, 20).map(p => p.id));
    renderGrid();
  });

  $('#carousel-clear').addEventListener('click', () => {
    selectedIds.clear();
    renderGrid();
  });

  $('#carousel-form').addEventListener('submit', async e => {
    e.preventDefault();
    errEl.textContent = '';
    msgEl.textContent = '';
    if (selectedIds.size < minProducts) {
      errEl.textContent = `Selecciona al menos ${minProducts} productos para el carrusel.`;
      return;
    }
    const orderedIds = allProducts.filter(p => selectedIds.has(p.id)).map(p => p.id);
    const btn = e.currentTarget.querySelector('[type="submit"]');
    await busy(btn, async () => {
      try {
        const res = await api('/admin/carousel', {
          method: 'PUT',
          body: {
            productIds: orderedIds,
            speedSeconds: Number(speedNum.value),
            direction: dirSelect.value
          }
        });
        selectedIds = new Set(res.productIds || []);
        syncSpeed(res.speedSeconds);
        dirSelect.value = res.direction || 'left';
        msgEl.textContent = `Carrusel guardado (${selectedIds.size} productos, ${res.speedSeconds}s, dirección ${res.direction === 'right' ? 'derecha' : 'izquierda'}).`;
        renderGrid();
      } catch (error) {
        errEl.textContent = error.message || 'No se pudo guardar el carrusel.';
      }
    });
  });

  (async () => {
    const { user } = await Cielo.session();
    if (!user) { location.replace('/acceso/login.html?next=/admin/admin-carrusel.html'); return; }
    if (user.role !== 'ADMIN') { location.replace('/tienda/index.html'); return; }
    $('#dashboard').hidden = false;
    const [prods, cfg] = await Promise.all([api('/products'), api('/carousel')]);
    allProducts = prods.map(p => ({ ...p, price: Number(p.price) }));
    minProducts = Number(cfg.minProducts || 8);
    selectedIds = new Set((cfg.productIds || []).map(Number));
    syncSpeed(cfg.speedSeconds || 55);
    dirSelect.value = cfg.direction === 'right' ? 'right' : 'left';
    renderGrid();
  })().catch(error => { errEl.textContent = error.message; });
})();
