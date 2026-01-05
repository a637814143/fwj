<template>
  <div class="hero-carousel">
    <transition name="carousel-fade">
      <div
        :key="activeSlide.src"
        class="carousel-backdrop"
        :style="{ backgroundImage: `linear-gradient(135deg, rgba(9, 24, 44, 0.35), rgba(9, 24, 44, 0.18)), url(${activeSlide.src})` }"
        aria-hidden="true"
      ></div>
    </transition>

    <div class="hero-content">
      <slot />

      <div v-if="!hasDefaultSlot" class="carousel-overlay">
        <h3>{{ activeSlide.title }}</h3>
        <p>{{ activeSlide.caption }}</p>
      </div>

      <button class="nav prev" type="button" aria-label="上一张" @click="prevSlide">‹</button>
      <button class="nav next" type="button" aria-label="下一张" @click="nextSlide">›</button>
    </div>

    <div class="carousel-dots" role="tablist" aria-label="示例房源展示">
      <button
        v-for="(slide, index) in slides"
        :key="slide.src"
        :class="['dot', { active: index === activeIndex } ]"
        type="button"
        role="tab"
        :aria-selected="index === activeIndex"
        @click="setSlide(index)"
      ></button>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, useSlots } from 'vue';

const makeSvgDataUri = (id, palette, accent) => {
  const [c1, c2, c3] = palette;
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1600 960">
      <defs>
        <linearGradient id="g-${id}" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="${c1}" />
          <stop offset="55%" stop-color="${c2}" />
          <stop offset="100%" stop-color="${c3}" />
        </linearGradient>
      </defs>
      <rect width="1600" height="960" fill="url(#g-${id})" />
      <rect x="120" y="140" width="1360" height="680" rx="64" fill="${accent}" opacity="0.24" />
      <rect x="190" y="220" width="960" height="540" rx="44" fill="rgba(255,255,255,0.14)" />
      <rect x="1200" y="260" width="240" height="360" rx="36" fill="rgba(0,0,0,0.08)" />
      <circle cx="340" cy="640" r="110" fill="rgba(0,0,0,0.12)" />
      <circle cx="520" cy="600" r="120" fill="rgba(255,255,255,0.18)" />
      <circle cx="760" cy="620" r="90" fill="rgba(255,255,255,0.12)" />
    </svg>
  `;
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
};

const slides = [
  {
    src: makeSvgDataUri('warm-lounge', ['#f6e5d3', '#e5c4a5', '#d69b73'], 'rgba(126, 83, 53, 0.55)'),
    alt: '原木风客厅与阳光',
    title: '宽敞餐客一体',
    caption: '明亮木色与白纱窗景，让空间自然延展。'
  },
  {
    src: makeSvgDataUri('tan-sofa', ['#f4e9de', '#e2cdb2', '#c6a27b'], 'rgba(181, 137, 92, 0.52)'),
    alt: '米色沙发与拱形窗',
    title: '柔和拱窗起居室',
    caption: '圆角柜体与落地窗，把阳台景致引入室内。'
  },
  {
    src: makeSvgDataUri('golden-lounge', ['#f0e2cf', '#d7bf91', '#c79c63'], 'rgba(186, 142, 88, 0.54)'),
    alt: '挑高客厅与木梁',
    title: '挑高奢雅客厅',
    caption: '木梁与落地窗的层次，突出空间的通透与仪式感。'
  },
  {
    src: makeSvgDataUri('art-deco', ['#f1e6da', '#d0bfb1', '#9a8271'], 'rgba(122, 94, 74, 0.56)'),
    alt: '岩板与装饰画',
    title: '雅致会客厅',
    caption: '深色岩板与装饰画，营造沉稳又温暖的氛围。'
  },
  {
    src: makeSvgDataUri('night-city', ['#dbe9ff', '#bcd4f6', '#93b5ec'], 'rgba(58, 104, 171, 0.48)'),
    alt: '社区街景与暖色灯光',
    title: '社区街景民宿',
    caption: '街角暖光小屋，突出邻里氛围与夜色质感。'
  }
];

const activeIndex = ref(0);
const slots = useSlots();
const intervalMs = 5000;
let timerId;

const activeSlide = computed(() => slides[activeIndex.value]);
const hasDefaultSlot = computed(() => Boolean(slots.default));

const nextSlide = () => {
  activeIndex.value = (activeIndex.value + 1) % slides.length;
};

const prevSlide = () => {
  activeIndex.value = (activeIndex.value - 1 + slides.length) % slides.length;
};

const resetTimer = () => {
  if (timerId) {
    window.clearInterval(timerId);
  }
  timerId = window.setInterval(nextSlide, intervalMs);
};

const preloadImages = () => {
  slides.forEach(({ src }) => {
    const img = new Image();
    img.decoding = 'async';
    img.src = src;
  });
};

const setSlide = (index) => {
  activeIndex.value = index;
  resetTimer();
};

onMounted(() => {
  resetTimer();
  preloadImages();
});

onBeforeUnmount(() => {
  if (timerId) {
    window.clearInterval(timerId);
  }
});
</script>

<style scoped>
.hero-carousel {
  position: relative;
  overflow: hidden;
  border-radius: calc(var(--radius-lg) + 0.35rem);
  width: 100%;
  min-height: min(100vh, 960px);
  box-shadow: 0 26px 60px rgba(12, 35, 68, 0.22);
  color: #fff;
  padding: clamp(1.6rem, 4vw, 2.8rem);
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.carousel-backdrop {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  filter: saturate(1.02);
  transition: opacity 0.45s ease;
  pointer-events: none;
  will-change: opacity;
}

.hero-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  height: 100%;
}

.carousel-overlay {
  align-self: flex-start;
  background: rgba(0, 0, 0, 0.14);
  backdrop-filter: blur(1px);
  padding: 0.85rem 1rem;
  border-radius: calc(var(--radius-md) + 0.2rem);
  box-shadow: 0 14px 30px rgba(0, 0, 0, 0.18);
  max-width: min(460px, 70%);
}

.carousel-overlay h3 {
  margin: 0 0 0.25rem;
  font-size: 1.08rem;
  letter-spacing: 0.01em;
}

.carousel-overlay p {
  margin: 0;
  font-size: 0.92rem;
  color: rgba(255, 255, 255, 0.9);
}

.nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.86);
  color: var(--color-primary, #1d5ad7);
  font-size: 1.5rem;
  font-weight: 700;
  cursor: pointer;
  transition: transform var(--transition-base), box-shadow var(--transition-base), background 0.2s ease;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.18);
}

.nav:hover {
  transform: translateY(-50%) scale(1.05);
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.22);
  background: #fff;
}

.nav.prev {
  left: 0.85rem;
}

.nav.next {
  right: 0.85rem;
}

.carousel-dots {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 0.45rem;
  align-items: center;
  justify-content: center;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 1px solid color-mix(in srgb, var(--color-border, #d6e2f1) 90%, transparent);
  background: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  transition: background var(--transition-base), transform var(--transition-base), border-color var(--transition-base);
}

.dot.active {
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  border-color: transparent;
  transform: scale(1.05);
}

.carousel-fade-enter-active,
.carousel-fade-leave-active {
  transition: opacity 0.45s ease;
}

.carousel-fade-enter-from,
.carousel-fade-leave-to {
  opacity: 0;
}

@media (max-width: 780px) {
  .hero-carousel {
    padding: 1.25rem;
    min-height: 240px;
  }

  .nav {
    width: 38px;
    height: 38px;
  }

  .carousel-overlay {
    max-width: 100%;
  }
}

@media (max-width: 1180px) {
  .hero-carousel {
    min-height: auto;
  }
}
</style>
