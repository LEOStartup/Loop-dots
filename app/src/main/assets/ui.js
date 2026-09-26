/* Retain live controls on value changes. Animate navigation, never a re-render. */
'use strict';
const UI = (() => {
  const ease = 'cubic-bezier(.22,.8,.24,1)';
  const running = new WeakMap();
  const reduced = () => document.documentElement.dataset.motion === 'off' || matchMedia('(prefers-reduced-motion: reduce)').matches;
  function animate(el, frames, duration = 260) {
    if (!el) return Promise.resolve();
    running.get(el)?.cancel();
    if (reduced()) return Promise.resolve();
    const animation = el.animate(frames, {duration, easing:ease});
    running.set(el, animation);
    return animation.finished.catch(() => {}).then(() => {if (running.get(el) === animation) running.delete(el)});
  }
  function key(node) {
    if (node.nodeType !== 1) return '';
    return node.id || (node.dataset.card ? 'habit:'+node.dataset.card : node.dataset.date ? 'day:'+node.dataset.date : '');
  }
  function sync(old, next) {
    if (old.nodeType !== next.nodeType || old.nodeName !== next.nodeName || key(old) !== key(next)) {
      old.replaceWith(next.cloneNode(true)); return;
    }
    if (old.nodeType !== 1) {if (old.nodeValue !== next.nodeValue) old.nodeValue = next.nodeValue; return}
    // Expanded details, selection/caret, scroll and focus belong to the live UI.
    for (const attr of [...old.attributes]) {
      if (attr.name === 'open' && old.tagName === 'DETAILS') continue;
      if (!next.hasAttribute(attr.name)) old.removeAttribute(attr.name);
    }
    for (const attr of next.attributes) if (old.getAttribute(attr.name) !== attr.value) old.setAttribute(attr.name, attr.value);
    if (old instanceof HTMLInputElement || old instanceof HTMLTextAreaElement) {
      if (document.activeElement !== old && old.value !== next.value) old.value = next.value;
      if (old instanceof HTMLInputElement) old.checked = next.checked;
      return;
    }
    children(old, next);
    if (old instanceof HTMLSelectElement && document.activeElement !== old) old.value = next.value;
  }
  function children(parent, next) {
    let cursor = parent.firstChild;
    for (const wanted of [...next.childNodes]) {
      const id = key(wanted);
      if (id && key(cursor || {}) !== id) {
        const found = [...parent.childNodes].find(n => key(n) === id);
        if (found) {parent.insertBefore(found, cursor); cursor = found}
        else {parent.insertBefore(wanted.cloneNode(true), cursor); continue}
      }
      if (!cursor) {parent.append(wanted.cloneNode(true)); continue}
      const following = cursor.nextSibling;
      sync(cursor, wanted);
      cursor = following;
    }
    while (cursor) {const following = cursor.nextSibling; cursor.remove(); cursor = following}
  }
  function patch(el, html) {
    const template = document.createElement('template'); template.innerHTML = html;
    children(el, template.content);
  }
  const snapshots = new WeakMap();
  function swap(el, update, direction = 1, distance = 12) {
    const previous = snapshots.get(el);
    if (previous) {stop(previous);previous.remove()}
    stop(el);
    if (reduced()) {update(); return}
    const bounds = el.getBoundingClientRect(), style = getComputedStyle(el), copy = el.cloneNode(true);
    copy.removeAttribute('id'); copy.querySelectorAll('[id]').forEach(n=>n.removeAttribute('id'));
    copy.classList.add('transition-copy'); copy.setAttribute('aria-hidden','true'); copy.inert = true;
    const background = getComputedStyle(document.documentElement).getPropertyValue(el.closest('.sheet')?'--sheet':'--bg');
    Object.assign(copy.style,{position:'fixed',top:bounds.top+'px',left:bounds.left+'px',width:bounds.width+'px',height:bounds.height+'px',minHeight:'0',maxWidth:'none',padding:style.padding,margin:'0',background,opacity:'1',transform:'none',pointerEvents:'none',zIndex:el.closest('.sheet')?'34':'9'});
    document.body.append(copy); copy.scrollTop=el.scrollTop; snapshots.set(el,copy);
    update();
    // Only the opaque outgoing surface dissolves. The incoming page is always
    // fully painted, so there is no dark midpoint and no translated nav/layout.
    animate(copy,[{opacity:1},{opacity:0}],180).then(()=>copy.remove());
  }
  function pulse(el) {animate(el,[{transform:'scale(.9)'},{transform:'scale(1)'}],200)}
  function stop(el) {running.get(el)?.cancel();running.delete(el)}
  function cancelAll() {document.getAnimations().forEach(a=>a.finish())}
  return {patch, animate, swap, pulse, reduced, cancelAll, stop};
})();
