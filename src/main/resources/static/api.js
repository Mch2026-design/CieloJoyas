'use strict';
window.Cielo = (() => {
  let currentSession;
  async function session(refresh = false) {
    if (refresh) currentSession = null;
    if (!currentSession) currentSession = fetch('/api/auth/me', {credentials:'same-origin',cache:'no-store',headers:{Accept:'application/json'}})
      .then(async response => { if (!response.ok) throw Error('No se pudo consultar tu sesión. Vuelve a intentarlo.'); return response.json(); })
      .catch(error => { currentSession = null; throw error; });
    return currentSession;
  }
  async function api(path, options = {}) {
    const controller = new AbortController();
    const multipart = options.body instanceof FormData;
    const timer = setTimeout(() => controller.abort(), multipart ? 30000 : 15000);
    try {
      const headers = {'Accept':'application/json'};
      if (options.body !== undefined && !multipart) headers['Content-Type'] = 'application/json';
      if (options.method && !['GET','HEAD'].includes(options.method)) headers['X-CSRF-Token'] = (await session()).csrf;
      const response = await fetch('/api' + path, {...options, headers, credentials:'same-origin', signal:controller.signal,
        body: options.body === undefined ? undefined : multipart ? options.body : JSON.stringify(options.body)});
      const data = await response.json().catch(() => ({}));
      if (!response.ok) { const error = Error(data.error || 'No se pudo completar la operación. Vuelve a intentarlo.'); error.status=response.status; if(response.status===401||response.status===403)currentSession=null; throw error; }
      if (path.startsWith('/auth/')) currentSession = null;
      return data;
    } catch (error) {
      if (error.name === 'AbortError') throw Error('La conexión tardó demasiado. Vuelve a intentarlo.');
      if (error instanceof TypeError) throw Error('No hay conexión con la tienda. Comprueba que Java esté ejecutándose.');
      throw error;
    } finally { clearTimeout(timer); }
  }
  const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const money = value => new Intl.NumberFormat('es-PE',{style:'currency',currency:'PEN'}).format(Number(value || 0));
  const busy = async (button,fn) => { if(button.disabled)return;button.disabled=true;button.setAttribute('aria-busy','true');try{return await fn();}finally{button.disabled=false;button.removeAttribute('aria-busy');} };
  return {api,session,esc,money,busy};
})();
