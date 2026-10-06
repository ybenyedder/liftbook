export const CLICK_FN = `(() => {
  const clickText = (txt, yMax = 9999, yMin = -1, occurrence = 'last') => {
    let rows = [...document.querySelectorAll('div')].filter(d => d.textContent === txt && d.getBoundingClientRect().y < yMax && d.getBoundingClientRect().y > yMin);
    const el = occurrence === 'first' ? rows[0] : rows[rows.length - 1];
    if (!el) return 'notfound:' + txt;
    const r = el.getBoundingClientRect();
    const target = document.elementFromPoint(r.x + r.width / 2, r.y + r.height / 2);
    if (!target) return 'nopoint:' + txt;
    for (const t of ['pointerdown','mousedown','pointerup','mouseup','click']) {
      target.dispatchEvent(t.startsWith('pointer') ? new PointerEvent(t, { bubbles: true, cancelable: true, pointerId: 1, pointerType: 'mouse', isPrimary: true }) : new MouseEvent(t, { bubbles: true, cancelable: true }));
    }
    return 'ok';
  };
  const clickPoint = (x, y) => {
    const target = document.elementFromPoint(x, y);
    if (!target) return 'nopoint';
    for (const t of ['pointerdown','mousedown','pointerup','mouseup','click']) {
      target.dispatchEvent(t.startsWith('pointer') ? new PointerEvent(t, { bubbles: true, cancelable: true, pointerId: 1, pointerType: 'mouse', isPrimary: true }) : new MouseEvent(t, { bubbles: true, cancelable: true }));
    }
    return 'ok';
  };
  const snap = () => [...document.querySelectorAll('div')].map(d => d.textContent).filter(t => t != null && t !== '' && t.length < 90);
  return { clickText, clickPoint, snap, VERSION: 3 };
})()`;
