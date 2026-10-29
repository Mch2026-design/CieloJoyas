'use strict';
(() => {
  const {api,session,esc,busy}=Cielo;
  const form=document.querySelector('#access-form'),summary=document.querySelector('#access-error'),mode=document.body.dataset.access;
  const controls=[...form.querySelectorAll('input')];
  const destination=user=>user.role==='ADMIN'?'productos.html':'index.html';
  session().then(data=>{
    if(data.user){location.replace(destination(data.user));return;}
    if(mode==='setup'&&!data.needsAdmin){location.replace('login.html');return;}
    if(mode==='login')document.querySelector('#setup-invitation').hidden=!data.needsAdmin;
  }).catch(error=>showError(error.message));
  function showError(message){summary.textContent=message;summary.hidden=false;summary.focus();}
  function validate(input){
    let message='';const value=input.value;
    if(input.required&&!value.trim())message='Completa este campo.';
    else if(input.type==='email'&&value&&!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(value.trim()))message='Ingresa un correo electrónico válido.';
    else if(input.name==='password'&&mode!=='login'&&value.length<8)message='Usa al menos 8 caracteres.';
    else if(input.name==='passwordConfirmation'&&value!==form.elements.password.value)message='Las contraseñas deben coincidir.';
    else if(input.name==='phone'&&value&&!/^9\d{8}$/.test(value))message='Ingresa 9 dígitos que empiecen por 9.';
    else if(['firstName','lastName'].includes(input.name)&&value.trim().length<2)message='Ingresa al menos 2 caracteres.';
    document.querySelector('#error-'+input.name).textContent=message;
    input.setAttribute('aria-invalid',String(!!message));return message;
  }
  function normalize(input){
    if(input.name!=='password'&&input.name!=='passwordConfirmation')input.value=input.value.trim();
    if(input.name==='email')input.value=input.value.toLowerCase();
    if(input.name==='phone')input.value=input.value.replace(/\D/g,'').replace(/^51(?=9\d{8}$)/,'');
  }
  controls.forEach(input=>input.addEventListener('blur',event=>{
    normalize(input);
    if(!event.relatedTarget?.closest('button[type="submit"]'))validate(input);
  }));
  controls.forEach(input=>input.addEventListener('input',()=>{
    if(input.getAttribute('aria-invalid')==='true')validate(input);
    if(input.name==='password'&&form.elements.passwordConfirmation?.getAttribute('aria-invalid')==='true')validate(form.elements.passwordConfirmation);
  }));
  document.querySelectorAll('[data-password]').forEach(button=>button.addEventListener('click',()=>{
    const input=document.getElementById(button.dataset.password),visible=input.type==='password';input.type=visible?'text':'password';button.setAttribute('aria-pressed',String(visible));button.setAttribute('aria-label',visible?'Ocultar contraseña':'Mostrar contraseña');
  }));
  form.addEventListener('submit',async event=>{
    event.preventDefault();summary.hidden=true;
    controls.forEach(normalize);
    const errors=controls.map(input=>({input,message:validate(input)})).filter(x=>x.message);
    if(errors.length){summary.innerHTML='<p>Revisa los campos indicados para continuar.</p><ul>'+errors.map(({input,message})=>`<li><a href="#${input.id}">${esc(input.previousElementSibling?.textContent||input.closest('.access-field').querySelector('label').textContent)}: ${esc(message)}</a></li>`).join('')+'</ul>';summary.hidden=false;summary.focus();return;}
    const button=form.querySelector('[type=submit]'),original=button.innerHTML;
    await busy(button,async()=>{
      button.textContent=mode==='login'?'Iniciando sesión…':'Creando tu cuenta…';
      try{const data=await api('/auth/'+mode,{method:'POST',body:Object.fromEntries(new FormData(form))});location.assign(destination(data.user));}
      catch(error){showError(error.message);}
      finally{button.innerHTML=original;}
    });
  });
})();
