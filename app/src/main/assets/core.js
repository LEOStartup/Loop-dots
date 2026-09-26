/* Local calendar arithmetic deliberately avoids UTC date shifts and DST day lengths. */
const Core=(()=>{
const key=d=>`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
const date=s=>{const [y,m,d]=s.split('-').map(Number);return new Date(y,m-1,d,12)};
const add=(d,n)=>{let r=new Date(d);r.setDate(r.getDate()+n);return r};
const today=()=>new Date(new Date().getFullYear(),new Date().getMonth(),new Date().getDate(),12);
const value=(h,k)=>Math.max(0,Number((h.entries||{})[k]||0));
const done=(h,k)=>value(h,k)>=Math.max(1,Number(h.target||1));
const eligible=(h,d)=>!(h.schedule?.length)||h.schedule.includes(d.getDay());
function streak(h,now=today()){
 let days=Object.keys(h.entries||{}).filter(k=>done(h,k)&&date(k)<=now).sort();let best=0,n=0,last=null;
 for(let k of days){let d=date(k);if(!eligible(h,d))continue;let prev=add(d,-1);while(!eligible(h,prev))prev=add(prev,-1);n=last===key(prev)?n+1:1;best=Math.max(best,n);last=k}
 let d=now;while(!eligible(h,d))d=add(d,-1);if(!done(h,key(d))) {d=add(d,-1);while(!eligible(h,d))d=add(d,-1)}
 let current=0;for(let i=0;i<40000&&done(h,key(d));i++){current++;d=add(d,-1);while(!eligible(h,d))d=add(d,-1)}
 return {current,best,total:days.length}
}
function periodStreak(h,type,now=today()){
 const id=d=>type==='month'?d.getFullYear()*12+d.getMonth():Math.floor((Date.UTC(d.getFullYear(),d.getMonth(),d.getDate())/86400000+3)/7);
 let counts={};for(const k of Object.keys(h.entries||{}))if(done(h,k)&&date(k)<=now){let p=id(date(k));counts[p]=(counts[p]||0)+1}
 let min=h.goalPeriod===type?Math.max(1,Number(h.goalCount||1)):1;
 let ids=Object.keys(counts).map(Number).filter(p=>counts[p]>=min).sort((a,b)=>a-b),best=0,n=0,prev=null;
 for(let p of ids){n=p===prev+1?n+1:1;best=Math.max(best,n);prev=p}let p=id(now);if(!ids.includes(p))p--;let current=0;while(ids.includes(p)){current++;p--}return {current,best}
}
function rate(h,now=today()){
 let entries=Object.keys(h.entries||{}).filter(k=>date(k)<=now),start=h.created||key(now);if(entries.length)start=[start,...entries].sort()[0];
 let possible=0,completed=0;for(let d=date(start),i=0;d<=now&&i<40000;d=add(d,1),i++){if(eligible(h,d)){possible++;if(done(h,key(d)))completed++}}
 return possible?Math.round(completed/possible*10000)/100:0
}
function validate(s){
 if(!s||!Array.isArray(s.habits)||s.habits.length>10000)throw Error('Arquivo inválido');let ids=new Set();
 for(let h of s.habits){
  if(!h||typeof h.id!=='string'||!/^[A-Za-z0-9_-]{1,100}$/.test(h.id)||ids.has(h.id)||typeof h.name!=='string'||h.name.length>300)throw Error('Hábito inválido');ids.add(h.id);
  if(!/^#[0-9a-f]{6}$/i.test(h.color||''))h.color='#ef4444';
  if(!h.entries||typeof h.entries!=='object'||Array.isArray(h.entries))h.entries={};
  for(let [k,v] of Object.entries(h.entries))if(!/^\d{4}-\d{2}-\d{2}$/.test(k)||key(date(k))!==k||!Number.isFinite(Number(v))||v<0||v>999999)throw Error('Registro inválido');
  h.target=Math.max(1,Math.min(9999,Number(h.target)||1));h.description=String(h.description||'').slice(0,500);h.icon=String(h.icon||'✓').slice(0,30);
  h.schedule=Array.isArray(h.schedule)?h.schedule.filter(x=>Number.isInteger(x)&&x>=0&&x<=6):[];
  h.reminderDays=Array.isArray(h.reminderDays)?h.reminderDays.filter(x=>Number.isInteger(x)&&x>=0&&x<=6):[];
  h.reminderTime=/^([01]\d|2[0-3]):[0-5]\d$/.test(h.reminderTime)?h.reminderTime:'12:00';
  h.categories=Array.isArray(h.categories)?h.categories.filter(x=>typeof x==='string').map(x=>x.slice(0,40)):[];
  if(!h.notes||typeof h.notes!=='object'||Array.isArray(h.notes))h.notes={};
  for(let k of Object.keys(h.notes))h.notes[k]=String(h.notes[k]).slice(0,10000);
  if(!/^\d{4}-\d{2}-\d{2}$/.test(h.created||''))h.created=key(today());
 }
 s.settings=s.settings&&typeof s.settings==='object'?s.settings:{};
 if(!['month','week','year','stats'].includes(s.settings.start))s.settings.start='month';
 s.categories=Array.isArray(s.categories)?s.categories.filter(x=>typeof x==='string').map(x=>x.slice(0,40)):['Arte','Estudo','Finanças','Fitness','Nutrição','Saúde','Social','Trabalho','Outro','Manhã','Dia','Noite'];
 return s;
}
return {key,date,add,today,value,done,streak,periodStreak,rate,validate,eligible};
})();
if(typeof module!=='undefined')module.exports=Core;
