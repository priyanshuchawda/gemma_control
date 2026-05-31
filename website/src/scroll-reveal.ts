/**
 * Intersection Observer-based scroll reveal.
 * Elements with [data-reveal] fade + slide in when they enter the viewport.
 * Feature cards get a staggered delay based on their index.
 */
export function initScrollReveal(): void {
  const observer = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          const el = entry.target as HTMLElement;
          el.classList.add("revealed");
          observer.unobserve(el);
        }
      }
    },
    { threshold: 0.12, rootMargin: "0px 0px -40px 0px" }
  );

  // Reveal [data-reveal] elements
  document.querySelectorAll<HTMLElement>("[data-reveal]").forEach((el) => {
    observer.observe(el);
  });

  // Feature cards — staggered
  document.querySelectorAll<HTMLElement>(".feature-card").forEach((card, i) => {
    card.style.transitionDelay = `${i * 70}ms`;
    observer.observe(card);
  });

  // Install steps — staggered slide-in
  document.querySelectorAll<HTMLElement>(".install-steps li").forEach((li, i) => {
    li.style.transitionDelay = `${i * 80}ms`;
    observer.observe(li);
  });
}
