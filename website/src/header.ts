export function bindHeaderScrollState(): void {
  const header = document.querySelector<HTMLElement>(".site-header");
  if (!header) return;

  const onScroll = (): void => {
    header.toggleAttribute("data-scrolled", window.scrollY > 8);
  };

  onScroll();
  window.addEventListener("scroll", onScroll, { passive: true });
}
