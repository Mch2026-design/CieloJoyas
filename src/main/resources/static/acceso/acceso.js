'use strict';
(() => {
  const {api,session,esc,busy}=Cielo;
  const form=document.querySelector('#access-form'),summary=document.querySelector('#access-error'),mode=document.body.dataset.access;
  const googleWrap=document.querySelector('#google-auth-wrap'),googleBtn=document.querySelector('#google-login-btn');
  const controls=[...form.querySelectorAll('input')];
  const NEXT_KEY='cielo-auth-next';
  const safeNext=raw=>{
    const value=(raw||'').trim();
    if(!value||/^[a-zA-Z][a-zA-Z0-9+.-]*:|^\/\/|^\\/.test(value))return '';
    return '/'+value.replace(/^\/+/,'');
  };
  const currentNext=()=>safeNext(new URLSearchParams(location.search).get('next'));
  const readSavedNext=()=>{try{return safeNext(sessionStorage.getItem(NEXT_KEY));}catch{return '';}};
  const clearSavedNext=()=>{try{sessionStorage.removeItem(NEXT_KEY);}catch{}};
  const destination=(user,explicitNext)=>{
    if(user.role==='ADMIN')return '/admin/productos.html';
    return safeNext(explicitNext)||currentNext()||readSavedNext()||'/tienda/index.html';
  };
  const propagateNext=()=>{
    const next=currentNext();
    if(!next)return;
    document.querySelectorAll('a[href*="/acceso/login.html"],a[href*="/acceso/registro.html"],a[href^="login.html"],a[href^="registro.html"]').forEach(link=>{
      const url=new URL(link.getAttribute('href'),location.href);
      if(!url.searchParams.has('next'))url.searchParams.set('next',next);
      link.setAttribute('href',url.pathname+url.search);
    });
  };
  const cleanOAuthUrl=savedNext=>{
    const clean=new URL(location.pathname,location.origin);
    const keepNext=currentNext()||safeNext(savedNext);
    if(keepNext)clean.searchParams.set('next',keepNext);
    history.replaceState(null,'',clean.pathname+clean.search);
  };
  const handleGoogleCallback=async()=>{
    const hashParams=new URLSearchParams(location.hash.replace(/^#/,''));
    const queryParams=new URLSearchParams(location.search);
    const oauthError=hashParams.get('error')||queryParams.get('error');
    const oauthErrorDesc=hashParams.get('error_description')||queryParams.get('error_description');
    const accessToken=hashParams.get('access_token');
    if(!oauthError&&!accessToken)return false;
    const savedNext=readSavedNext();
    cleanOAuthUrl(savedNext);
    if(oauthError){
      const msg=oauthError==='access_denied'
        ?'Se canceló el inicio de sesión con Google.'
        :(oauthErrorDesc||'No se pudo completar el inicio de sesión con Google.');
      showError(msg);
      return true;
    }
    const original=googleBtn?googleBtn.innerHTML:'';
    if(googleBtn){googleBtn.disabled=true;googleBtn.textContent='Validando cuenta de Google…';}
    try{
      const data=await api('/auth/google',{method:'POST',body:{accessToken}});
      clearSavedNext();
      location.replace(destination(data.user,savedNext));
    }catch(error){
      showError(error.message||'No se pudo validar tu cuenta de Google.');
      if(googleBtn){googleBtn.disabled=false;googleBtn.innerHTML=original;}
    }
    return true;
  };
  const initGoogleAuth=()=>{
    if(!googleWrap||!googleBtn)return;
    api('/auth/google/config').then(config=>{
      if(!config||!config.supabaseUrl)return;
      googleWrap.hidden=false;
      googleBtn.addEventListener('click',()=>{
        if(!config.enabled){
          showError('Falta configurar SUPABASE_ANON_KEY en el servidor para activar Google.');
          return;
        }
        const nextVal=currentNext();
        try{
          if(nextVal)sessionStorage.setItem(NEXT_KEY,nextVal);
          else sessionStorage.removeItem(NEXT_KEY);
        }catch{}
        googleBtn.disabled=true;
        googleBtn.setAttribute('aria-busy','true');
        const targetPage=mode==='register'?'/acceso/registro.html':'/acceso/login.html';
        const redirectTo=`${location.origin}${targetPage}`;
        const baseUrl=String(config.supabaseUrl).replace(/\/+$/,'');
        location.assign(`${baseUrl}/auth/v1/authorize?provider=google&redirect_to=${encodeURIComponent(redirectTo)}`);
      });
    }).catch(()=>{});
  };
  propagateNext();
  initGoogleAuth();
  handleGoogleCallback().then(handled=>{
    if(handled)return;
    session().then(data=>{
      if(data.user){clearSavedNext();location.replace(destination(data.user));return;}
      if(mode==='setup'&&!data.needsAdmin){location.replace('/acceso/login.html');return;}
      if(mode==='login')document.querySelector('#setup-invitation').hidden=!data.needsAdmin;
    }).catch(error=>showError(error.message));
  });
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
      try{const data=await api('/auth/'+mode,{method:'POST',body:Object.fromEntries(new FormData(form))});clearSavedNext();location.assign(destination(data.user));}
      catch(error){showError(error.message);}
      finally{button.innerHTML=original;}
    });
  });
})();
