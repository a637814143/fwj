<template>
  <div class="hero-carousel">
    <div class="carousel-window">
      <transition name="carousel-fade" mode="out-in">
        <img
          :key="activeSlide.src"
          class="carousel-image"
          :src="activeSlide.src"
          :alt="activeSlide.alt"
          loading="lazy"
        />
      </transition>
      <div class="carousel-overlay">
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
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';

const slides = [
  {
    src: 'https://images.unsplash.com/photo-1523419400524-223c6f35123b?auto=format&fit=crop&w=1400&q=80',
    alt: '社区街景与暖色灯光',
    title: '社区街景民宿',
    caption: '街角暖光小屋，突出邻里氛围与夜色质感。'
  },
  {
    src: 'https://images.unsplash.com/photo-1449158743715-0a90ebb6d2d8?auto=format&fit=crop&w=1400&q=80',
    alt: '现代感别墅外立面',
    title: '现代几何别墅',
    caption: '大面积玻璃与悬挑结构，演绎现代生活美学。'
  },
  {
    src: 'https://images.unsplash.com/photo-1505691938895-1758d7feb511?auto=format&fit=crop&w=1400&q=80&sat=-30',
    alt: '落地窗客厅与壁炉',
    title: '温馨客厅一角',
    caption: '落地窗、壁炉与舒适沙发组合，展现家的温度。'
  },
  {
    src: 'https://images.unsplash.com/photo-1501183638710-841dd1904471?auto=format&fit=crop&w=1400&q=80',
    alt: '夜色中的玻璃房屋',
    title: '玻璃幕墙住宅',
    caption: '通透明亮的立面设计，让夜晚同样温馨。'
  },
  {
    src: 'https://images.unsplash.com/photo-1473186578172-c141e6798cf4?auto=format&fit=crop&w=1400&q=80',
    alt: '田园老屋与树林',
    title: '乡野田园风光',
    caption: '静谧老屋与树林阳光，营造悠然慢生活。'
  }
];

const activeIndex = ref(0);
const intervalMs = 5000;
let timerId;

const activeSlide = computed(() => slides[activeIndex.value]);

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

const setSlide = (index) => {
  activeIndex.value = index;
  resetTimer();
};

onMounted(() => {
  resetTimer();
});

onBeforeUnmount(() => {
  if (timerId) {
    window.clearInterval(timerId);
  }
});
</script>

<style scoped>
.hero-carousel {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  width: 100%;
}

.carousel-window {
  position: relative;
  overflow: hidden;
  border-radius: calc(var(--radius-lg) + 0.25rem);
  background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.16), transparent 65%),
    linear-gradient(145deg, rgba(28, 69, 120, 0.35), rgba(28, 69, 120, 0.6));
  aspect-ratio: 14 / 9;
  min-height: 280px;
  box-shadow: 0 24px 48px rgba(12, 35, 68, 0.18);
}

.carousel-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  filter: saturate(1.02);
}

.carousel-overlay {
  position: absolute;
  left: 1.4rem;
  bottom: 1.1rem;
  background: rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(4px);
  padding: 0.85rem 1rem;
  border-radius: calc(var(--radius-md) + 0.2rem);
  color: #fff;
  box-shadow: 0 14px 30px rgba(0, 0, 0, 0.18);
  max-width: min(420px, 70%);
}

.carousel-overlay h3 {
  margin: 0 0 0.25rem;
  font-size: 1.05rem;
  letter-spacing: 0.01em;
}

.carousel-overlay p {
  margin: 0;
  font-size: 0.9rem;
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
  background: rgba(255, 255, 255, 0.82);
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
  .carousel-window {
    min-height: 200px;
  }

  .carousel-overlay {
    left: 1rem;
    right: 1rem;
    max-width: none;
  }
}
</style>
