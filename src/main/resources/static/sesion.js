'use strict';
(() => {
  const headerLinks = [...document.querySelectorAll('[data-account-link]')];
  Cielo.session().then(({user}) => {
    for (const link of headerLinks) {
      const label = user ? user.role==='ADMIN' ? 'Admin' : (user.firstName || user.name || 'Tu cuenta').trim().split(/\s+/)[0] : 'Iniciar sesión';
      link.href = user ? user.role==='ADMIN' ? 'productos.html' : 'cuenta.html' : 'login.html';
      link.setAttribute('aria-label',label); link.title=label;
      link.querySelector('[data-account-label]').textContent=label;
    }
    if (user) {
      document.body.classList.add('has-session');
      const form=document.querySelector('#checkout-form');
      if(form) {
        for(const [field,value] of Object.entries({email:user.email,firstName:user.firstName,lastName:user.lastName,phone:user.phone}))
          if(value && !form.elements[field].value)form.elements[field].value=value;
        form.elements.email.readOnly=true;
      }
      document.querySelectorAll('[data-logout]').forEach(button=>button.hidden=false);


    }
  }).catch(() => {});
  document.querySelectorAll('[data-logout]').forEach(button=>button.addEventListener('click',()=>Cielo.busy(button,async()=>{
    try { await Cielo.api('/auth/logout',{method:'POST'}); location.assign('index.html'); }
    catch(error){const message=document.querySelector('[data-session-error]');if(message)message.textContent=error.message;else{const alert=document.createElement('p');alert.className='session-alert';alert.setAttribute('role','alert');alert.textContent=error.message;document.body.append(alert);setTimeout(()=>alert.remove(),8000);}}
  })));
})();
