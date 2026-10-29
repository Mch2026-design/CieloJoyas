'use strict';
(() => {
  // Contacto público: funciona aunque no exista sesión o la API no responda.
  if (document.body.classList.contains('access-page') || document.body.dataset.access || document.body.dataset.adminPage || document.body.classList.contains('admin-body') || /^\/(?:admin(?:-[a-z-]+)?|productos)\.html$/.test(location.pathname) || document.body.dataset.page==='cuenta' || location.pathname.endsWith('/cuenta.html') || document.querySelector('.whatsapp-float')) return;
  const contact=document.createElement('a');
  contact.className='whatsapp-float';
  contact.href='https://wa.me/51900934669';
  contact.target='_blank';
  contact.rel='noopener noreferrer';
  contact.setAttribute('aria-label','WhatsApp: hola, contáctanos al 900 934 669. Se abre en una pestaña nueva.');
  contact.innerHTML='<span class="whatsapp-float-icon" aria-hidden="true"></span><span class="whatsapp-float-copy"><small>WHATSAPP</small><strong>Hola, ¿te ayudamos?</strong><span>Contáctanos al <b>900 934 669</b></span></span>';
  document.body.append(contact);
})();
