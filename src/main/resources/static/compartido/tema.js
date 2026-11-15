'use strict';
(() => {
  const STORAGE_KEY = 'cielo_theme';
  const DARK = 'dark';
  const LIGHT = 'light';

  const SUN_SVG = '<svg viewBox="0 0 24 24" width="21" height="21" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41"/></svg>';
  const MOON_SVG = '<svg viewBox="0 0 24 24" width="21" height="21" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79Z"/></svg>';

  function getSavedTheme() {
    try {
      return localStorage.getItem(STORAGE_KEY) === DARK ? DARK : LIGHT;
    } catch {
      return LIGHT;
    }
  }

  function applyTheme(theme) {
    const isDark = theme === DARK;
    document.documentElement.setAttribute('data-theme', theme);
    document.documentElement.style.colorScheme = theme;
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute('content', isDark ? '#141210' : '#f7f3ef');
    document.querySelectorAll('[data-theme-toggle]').forEach(btn => {
      btn.innerHTML = isDark ? MOON_SVG : SUN_SVG;
      const label = isDark ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro';
      btn.setAttribute('aria-label', label);
      btn.setAttribute('title', label);
      btn.setAttribute('aria-pressed', isDark ? 'true' : 'false');
    });
  }

  function saveTheme(theme) {
    try {
      localStorage.setItem(STORAGE_KEY, theme);
    } catch {
      // ignore storage errors in restricted contexts
    }
  }

  function toggleTheme() {
    const next = document.documentElement.getAttribute('data-theme') === DARK ? LIGHT : DARK;
    saveTheme(next);
    applyTheme(next);
  }

  function mountToggle() {
    if (!document.querySelector('[data-theme-toggle]')) {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'icon-btn theme-toggle-btn';
      btn.setAttribute('data-theme-toggle', '');

      const accountLink = document.querySelector('[data-account-link]');
      if (accountLink && accountLink.parentElement) {
        accountLink.insertAdjacentElement('beforebegin', btn);
      } else {
        const accessBack = document.querySelector('.access-header .access-back');
        if (accessBack && accessBack.parentElement) {
          accessBack.insertAdjacentElement('beforebegin', btn);
        }
      }
    }

    document.querySelectorAll('[data-theme-toggle]').forEach(btn => {
      if (!btn.dataset.themeBound) {
        btn.dataset.themeBound = '1';
        btn.addEventListener('click', toggleTheme);
      }
    });

    applyTheme(getSavedTheme());
  }

  applyTheme(getSavedTheme());
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', mountToggle);
  } else {
    mountToggle();
  }
})();
