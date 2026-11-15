'use strict';
(() => {
  const { api, esc, money, busy } = Cielo;
  const $ = s => document.querySelector(s);

  let allProducts = [];
  let minProducts = 8;
  let maxCarousels = 5;
  let carousels = [];

  const msgEl = $('#carousel-message');
  const errEl = $('#carousel-error');
  const listEl = $('#carousel-list');
  const addBtn = $('#carousel-add-btn');
  const limitNoteEl = $('#carousel-limit-note');

  function clampSpeed(val) {
    return Math.max(10, Math.min(180, Number(val) || 55));
  }

  function pillText(c) {
    const n = c.productIds.size;
    const dirLabel = c.direction === 'right' ? 'Derecha →' : '← Izquierda';
    return `${n} ${n === 1 ? 'producto' : 'productos'} · ${c.speedSeconds}s · ${dirLabel}`;
  }

  function countText(c) {
    const n = c.productIds.size;
    return `(${n} ${n === 1 ? 'seleccionado' : 'seleccionados'} · mínimo ${minProducts})`;
  }

  function filteredProducts(search) {
    const q = (search || '').trim().toLocaleLowerCase('es');
    return allProducts.filter(p => !q || `${p.name} ${p.category} ${p.reference || ''}`.toLocaleLowerCase('es').includes(q));
  }

  function renderGridHtml(c) {
    return filteredProducts(c.search).map(p => `
      <label class="carousel-item-option">
        <input type="checkbox" value="${p.id}" ${c.productIds.has(p.id) ? 'checked' : ''}>
        <img src="${esc(p.src)}" alt="" width="48" height="58" loading="lazy">
        <span class="carousel-item-info">
          <strong>${esc(p.name)}</strong>
          <small>${esc(p.category)} · ${money(p.price)}</small>
        </span>
      </label>
    `).join('');
  }

  function updateSummaryAndCount(card, c) {
    const pill = card.querySelector('[data-pill]');
    if (pill) pill.textContent = pillText(c);
    const count = card.querySelector('[data-count]');
    if (count) {
      count.textContent = countText(c);
      count.style.color = c.productIds.size < minProducts ? '#9d2b24' : '#1f5126';
    }
  }

  function updateAddControls() {
    const total = carousels.length;
    addBtn.disabled = total >= maxCarousels;
    limitNoteEl.textContent = `${total} de ${maxCarousels} ${maxCarousels === 1 ? 'carrusel' : 'carruseles'} configurados`;
  }

  function renderAll() {
    listEl.innerHTML = carousels.map((c, idx) => `
      <details class="carousel-accordion" data-index="${idx}" ${c.open ? 'open' : ''}>
        <summary>
          <span class="carousel-accordion-title">
            <strong>Carrusel ${idx + 1}</strong>
            <span class="carousel-pill" data-pill>${pillText(c)}</span>
          </span>
          <span class="carousel-summary-right">
            ${idx > 0 ? `<button type="button" class="carousel-remove-btn" data-remove="${idx}">Eliminar</button>` : ''}
            <svg class="carousel-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="m6 9 6 6 6-6"/></svg>
          </span>
        </summary>
        <div class="carousel-accordion-body">
          <div class="carousel-controls-grid">
            <label>Dirección de desplazamiento
              <select class="carousel-dir-select" data-dir>
                <option value="left" ${c.direction === 'left' ? 'selected' : ''}>Hacia la izquierda (←)</option>
                <option value="right" ${c.direction === 'right' ? 'selected' : ''}>Hacia la derecha (→)</option>
              </select>
            </label>
            <label>Velocidad (segundos por vuelta: <b data-speed-label>${c.speedSeconds}s</b>)
              <div class="carousel-speed-row">
                <input class="carousel-speed-range" data-speed-range type="range" min="10" max="180" step="5" value="${c.speedSeconds}">
                <input class="carousel-speed-num" data-speed-num type="number" min="10" max="180" value="${c.speedSeconds}" aria-label="Segundos por vuelta del carrusel ${idx + 1}">
              </div>
              <small>Menor número = más rápido · Mayor número = más lento</small>
            </label>
          </div>
          <h2>Productos del carrusel <small data-count style="color:${c.productIds.size < minProducts ? '#9d2b24' : '#1f5126'}">${countText(c)}</small></h2>
          <div class="carousel-toolbar">
            <input class="carousel-search-input" data-search type="search" placeholder="Buscar producto o categoría…" value="${esc(c.search)}">
            <div style="display:flex;gap:8px;flex-wrap:wrap">
              <button type="button" class="carousel-secondary-btn" data-pick-20="${idx}">Elegir 20 primeros</button>
              <button type="button" class="carousel-secondary-btn" data-clear="${idx}">Limpiar selección</button>
            </div>
          </div>
          <div class="carousel-product-grid" data-grid aria-live="polite">${renderGridHtml(c)}</div>
        </div>
      </details>
    `).join('');
    updateAddControls();
  }

  listEl.addEventListener('toggle', e => {
    const details = e.target;
    if (!details || !details.matches('details.carousel-accordion')) return;
    const idx = Number(details.dataset.index);
    if (carousels[idx]) carousels[idx].open = details.open;
  }, true);

  listEl.addEventListener('input', e => {
    const card = e.target.closest('details.carousel-accordion');
    if (!card) return;
    const idx = Number(card.dataset.index);
    const c = carousels[idx];
    if (!c) return;

    if (e.target.matches('[data-speed-range]') || e.target.matches('[data-speed-num]')) {
      const clamped = clampSpeed(e.target.value);
      c.speedSeconds = clamped;
      card.querySelector('[data-speed-range]').value = clamped;
      card.querySelector('[data-speed-num]').value = clamped;
      card.querySelector('[data-speed-label]').textContent = clamped + 's';
      updateSummaryAndCount(card, c);
      return;
    }

    if (e.target.matches('[data-search]')) {
      c.search = e.target.value;
      card.querySelector('[data-grid]').innerHTML = renderGridHtml(c);
    }
  });

  listEl.addEventListener('change', e => {
    const card = e.target.closest('details.carousel-accordion');
    if (!card) return;
    const idx = Number(card.dataset.index);
    const c = carousels[idx];
    if (!c) return;

    if (e.target.matches('[data-dir]')) {
      c.direction = e.target.value === 'right' ? 'right' : 'left';
      updateSummaryAndCount(card, c);
      return;
    }

    const cb = e.target.closest('input[type="checkbox"]');
    if (cb) {
      const id = Number(cb.value);
      if (cb.checked) c.productIds.add(id);
      else c.productIds.delete(id);
      updateSummaryAndCount(card, c);
    }
  });

  listEl.addEventListener('click', e => {
    const removeBtn = e.target.closest('[data-remove]');
    if (removeBtn) {
      e.preventDefault();
      e.stopPropagation();
      const idx = Number(removeBtn.dataset.remove);
      if (idx > 0 && carousels.length > 1) {
        carousels.splice(idx, 1);
        renderAll();
      }
      return;
    }

    const pickBtn = e.target.closest('[data-pick-20]');
    if (pickBtn) {
      const idx = Number(pickBtn.dataset.pick20);
      const c = carousels[idx];
      if (!c) return;
      c.productIds = new Set(allProducts.slice(0, 20).map(p => p.id));
      const card = pickBtn.closest('details.carousel-accordion');
      card.querySelector('[data-grid]').innerHTML = renderGridHtml(c);
      updateSummaryAndCount(card, c);
      return;
    }

    const clearBtn = e.target.closest('[data-clear]');
    if (clearBtn) {
      const idx = Number(clearBtn.dataset.clear);
      const c = carousels[idx];
      if (!c) return;
      c.productIds.clear();
      const card = clearBtn.closest('details.carousel-accordion');
      card.querySelector('[data-grid]').innerHTML = renderGridHtml(c);
      updateSummaryAndCount(card, c);
    }
  });

  addBtn.addEventListener('click', () => {
    errEl.textContent = '';
    msgEl.textContent = '';
    if (carousels.length >= maxCarousels) return;
    carousels.push({
      productIds: new Set(allProducts.slice(0, 20).map(p => p.id)),
      speedSeconds: 55,
      direction: carousels.length % 2 === 1 ? 'right' : 'left',
      search: '',
      open: true
    });
    renderAll();
  });

  $('#carousel-form').addEventListener('submit', async e => {
    e.preventDefault();
    errEl.textContent = '';
    msgEl.textContent = '';

    for (let i = 0; i < carousels.length; i++) {
      if (carousels[i].productIds.size < minProducts) {
        carousels[i].open = true;
        renderAll();
        errEl.textContent = `Carrusel ${i + 1}: selecciona al menos ${minProducts} productos.`;
        return;
      }
    }

    const payload = carousels.map(c => ({
      productIds: allProducts.filter(p => c.productIds.has(p.id)).map(p => p.id),
      speedSeconds: clampSpeed(c.speedSeconds),
      direction: c.direction === 'right' ? 'right' : 'left'
    }));

    const btn = e.currentTarget.querySelector('[type="submit"]');
    await busy(btn, async () => {
      try {
        const res = await api('/admin/carousel', {
          method: 'PUT',
          body: { carousels: payload }
        });
        const saved = Array.isArray(res.carousels) ? res.carousels : [];
        carousels = saved.map((c, idx) => ({
          productIds: new Set((c.productIds || []).map(Number)),
          speedSeconds: clampSpeed(c.speedSeconds),
          direction: c.direction === 'right' ? 'right' : 'left',
          search: carousels[idx]?.search || '',
          open: Boolean(carousels[idx]?.open)
        }));
        renderAll();
        msgEl.textContent = `Se ${carousels.length === 1 ? 'guardó 1 carrusel' : `guardaron ${carousels.length} carruseles`} correctamente.`;
      } catch (error) {
        errEl.textContent = error.message || 'No se pudieron guardar los carruseles.';
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
    maxCarousels = Number(cfg.maxCarousels || 5);
    const items = Array.isArray(cfg.carousels) && cfg.carousels.length
      ? cfg.carousels
      : [{ productIds: cfg.productIds || [], speedSeconds: cfg.speedSeconds || 55, direction: cfg.direction || 'left' }];
    carousels = items.slice(0, maxCarousels).map(c => ({
      productIds: new Set((c.productIds || []).map(Number)),
      speedSeconds: clampSpeed(c.speedSeconds),
      direction: c.direction === 'right' ? 'right' : 'left',
      search: '',
      open: false
    }));
    renderAll();
  })().catch(error => { errEl.textContent = error.message; });
})();
