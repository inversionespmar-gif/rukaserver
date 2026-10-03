const screens = {
  tv: { image: 'assets/app-tv.png', label: 'TV en vivo', alt: 'Pantalla real de televisión en vivo en RukaTV' },
  movies: { image: 'assets/app-movies.png', label: 'Películas', alt: 'Pantalla real del catálogo de películas de RukaTV' },
  series: { image: 'assets/app-series.png', label: 'Series', alt: 'Pantalla real de series y temporadas de RukaTV' }
};
const tabs = [...document.querySelectorAll('[data-screen]')];
function selectScreen(tab) {
  const screen = screens[tab.dataset.screen];
  tabs.forEach(item => {
    const selected = item === tab;
    item.setAttribute('aria-selected', String(selected));
    item.tabIndex = selected ? 0 : -1;
    document.getElementById(item.getAttribute('aria-controls')).hidden = !selected;
  });
  const image = document.getElementById('explorer-image');
  image.src = screen.image;
  image.alt = screen.alt;
  document.getElementById('explorer-label').textContent = screen.label;
}
tabs.forEach((tab, index) => {
  tab.addEventListener('click', () => selectScreen(tab));
  tab.addEventListener('keydown', event => {
    let next;
    if (event.key === 'ArrowRight') next = (index + 1) % tabs.length;
    if (event.key === 'ArrowLeft') next = (index + tabs.length - 1) % tabs.length;
    if (event.key === 'Home') next = 0;
    if (event.key === 'End') next = tabs.length - 1;
    if (next !== undefined) { event.preventDefault(); tabs[next].focus(); selectScreen(tabs[next]); }
  });
});
const menu = document.querySelector('.menu-toggle');
const navigation = document.getElementById('navigation');
function closeMenu() {
  navigation.classList.remove('open');
  menu.setAttribute('aria-expanded', 'false');
  menu.setAttribute('aria-label', 'Abrir menú');
}
menu.addEventListener('click', () => {
  const opened = navigation.classList.toggle('open');
  menu.setAttribute('aria-expanded', String(opened));
  menu.setAttribute('aria-label', opened ? 'Cerrar menú' : 'Abrir menú');
});
navigation.addEventListener('click', event => { if (event.target.closest('a,button')) closeMenu(); });
let returnFocus;
function openModal(modalId) {
  const modal = document.getElementById(modalId);
  if (!modal) return;
  returnFocus = document.activeElement;
  closeMenu();
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
  const focusTarget = navigation.contains(returnFocus) && getComputedStyle(navigation).display === 'none' ? menu : returnFocus;
  focusTarget?.focus();
}
document.addEventListener('click', event => {
  if (event.target.classList.contains('modal-overlay')) closeModal(event.target.id);
});
document.addEventListener('keydown', event => {
  const modal = document.querySelector('.modal-overlay.active');
  if (event.key === 'Escape') {
    if (modal) closeModal(modal.id);
    else if (navigation.classList.contains('open')) { closeMenu(); menu.focus(); }
  }
  if (!modal || event.key !== 'Tab') return;
  const controls = [...modal.querySelectorAll('button,a[href],input,select,textarea,[tabindex="0"]')];
  const first = controls[0], last = controls.at(-1);
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
});
