let returnFocus;
function openModal(modalId) {
  const modal = document.getElementById(modalId);
  if (!modal) return;
  returnFocus = document.activeElement;
  modal.classList.add('active');
  modal.setAttribute('role', 'dialog');
  modal.setAttribute('aria-modal', 'true');
  const title = modal.querySelector('h2');
  title.id = `${modalId}-title`;
  modal.setAttribute('aria-labelledby', title.id);
  document.body.style.overflow = 'hidden';
  modal.querySelector('button').focus();
}
function closeModal(modalId) {
  document.getElementById(modalId)?.classList.remove('active');
  document.body.style.overflow = '';
  returnFocus?.focus();
}
document.addEventListener('click', event => {
  if (event.target.classList.contains('modal-overlay')) closeModal(event.target.id);
});
document.addEventListener('keydown', event => {
  const modal = document.querySelector('.modal-overlay.active');
  if (!modal) return;
  if (event.key === 'Escape') closeModal(modal.id);
  if (event.key === 'Tab') {
    const controls = [...modal.querySelectorAll('button,a[href],input,select,textarea,[tabindex="0"]')];
    const first = controls[0], last = controls.at(-1);
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
  }
});
