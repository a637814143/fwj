<template>
  <div class="modal-backdrop" @click.self="emit('close')">
    <div class="modal">
      <header class="modal__header">
        <div class="modal__title">
          <h3>{{ house.title }}</h3>
          <p class="modal__address">{{ house.address }}</p>
        </div>
        <button type="button" class="icon-button" aria-label="关闭详情" @click="emit('close')">×</button>
      </header>

      <section class="modal__content">
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">价格</span>
            <span class="info-value">￥{{ formattedPrice }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">面积</span>
            <span class="info-value">{{ formattedArea }} ㎡</span>
          </div>
          <div class="info-item">
            <span class="info-label">挂牌日期</span>
            <span class="info-value">{{ formattedDate }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">楼层</span>
            <span class="info-value">{{ formattedFloor }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">卖家账号</span>
            <span class="info-value">{{ house.sellerUsername || '-' }}</span>
          </div>
        </div>

        <p v-if="house.description" class="modal__description">{{ house.description }}</p>

        <div v-if="keywordList.length" class="keywords">
          <h4>房源关键词</h4>
          <ul>
            <li v-for="keyword in keywordList" :key="keyword">{{ keyword }}</li>
          </ul>
        </div>

        <div class="gallery">
          <div class="gallery__header">
            <h4>图片展示</h4>
            <span v-if="images.length" class="gallery__count">共 {{ images.length }} 张</span>
          </div>
          <p v-if="!images.length" class="gallery__empty">暂无上传图片。</p>
          <div v-else class="gallery__grid">
            <figure v-for="(image, index) in images" :key="`${index}-${image.length}`" class="gallery__item">
              <img :src="image" :alt="`${house.title} 图片 ${index + 1}`" loading="lazy" />
              <figcaption>图片 {{ index + 1 }}</figcaption>
            </figure>
          </div>
        </div>
      </section>

      <footer class="modal__footer">
        <div class="contact">
          <span>卖家：{{ house.sellerName }}</span>
          <span>联系方式：{{ maskedPhone }}</span>
        </div>
        <button type="button" class="primary-button" @click="emit('close')">关闭</button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  house: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['close']);

const images = computed(() => (Array.isArray(props.house?.imageUrls) ? props.house.imageUrls : []));

const formattedPrice = computed(() => formatNumber(props.house?.price));
const formattedArea = computed(() => formatNumber(props.house?.area));
const formattedDate = computed(() => formatDate(props.house?.listingDate));
const formattedFloor = computed(() => {
  if (props.house?.floor == null || props.house.floor === '') {
    return '—';
  }
  return `${props.house.floor} 层`;
});
const keywordList = computed(() => (Array.isArray(props.house?.keywords) ? props.house.keywords : []));
const maskedPhone = computed(() => maskPhone(props.house?.contactNumber));

function formatNumber(value) {
  if (value == null || value === '') {
    return '-';
  }
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  });
}

function formatDate(value) {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleDateString('zh-CN');
}

function maskPhone(value) {
  if (!value) {
    return '-';
  }
  const phone = String(value).trim();
  if (phone.length <= 4) {
    return '*'.repeat(phone.length);
  }
  const prefix = phone.slice(0, Math.min(3, phone.length - 4));
  const suffix = phone.slice(-4);
  return `${prefix}****${suffix}`;
}
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(2, 6, 23, 0.65);
  backdrop-filter: blur(6px);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 1.5rem;
  z-index: 1000;
}

.modal {
  width: min(900px, 100%);
  max-height: 90vh;
  background: rgba(8, 15, 35, 0.95);
  border-radius: 28px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 35px 80px rgba(3, 7, 18, 0.55);
  border: 1px solid rgba(148, 163, 184, 0.2);
  color: var(--text-primary);
}

.modal__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 1.6rem 1.85rem;
  background: linear-gradient(135deg, rgba(30, 64, 175, 0.85), rgba(37, 99, 235, 0.85));
  color: var(--text-primary);
}

.modal__title h3 {
  margin: 0 0 0.3rem;
  font-size: 1.45rem;
  font-weight: 600;
}

.modal__address {
  margin: 0;
  font-size: 0.95rem;
  color: rgba(226, 232, 240, 0.85);
}

.icon-button {
  border: none;
  background: rgba(15, 23, 42, 0.3);
  color: var(--text-primary);
  font-size: 1.5rem;
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 50%;
  cursor: pointer;
  line-height: 2.25rem;
  transition: background 0.2s ease, transform 0.2s ease;
}

.icon-button:hover {
  background: rgba(15, 23, 42, 0.45);
  transform: translateY(-1px);
}

.modal__content {
  padding: 1.6rem 1.85rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1rem;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  background: rgba(15, 23, 42, 0.65);
  border-radius: 1rem;
  padding: 0.85rem 1rem;
  border: 1px solid rgba(148, 163, 184, 0.25);
}

.info-label {
  color: var(--text-muted);
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.info-value {
  font-weight: 600;
  color: var(--text-primary);
}

.modal__description {
  margin: 0;
  background: rgba(15, 23, 42, 0.6);
  border-radius: 1.1rem;
  padding: 1.1rem 1.25rem;
  color: var(--text-secondary);
  line-height: 1.65;
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.keywords {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.keywords h4,
.gallery h4 {
  margin: 0;
  color: var(--text-primary);
}

.keywords ul {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.keywords li {
  background: rgba(59, 130, 246, 0.25);
  color: var(--text-primary);
  padding: 0.3rem 0.75rem;
  border-radius: 999px;
  font-size: 0.85rem;
}

.gallery {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.gallery__header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.gallery__count {
  font-size: 0.85rem;
  color: var(--text-secondary);
}

.gallery__empty {
  margin: 0;
  padding: 1rem;
  border-radius: 0.9rem;
  background: rgba(15, 23, 42, 0.6);
  color: var(--text-secondary);
  text-align: center;
  border: 1px solid rgba(148, 163, 184, 0.25);
}

.gallery__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.gallery__item {
  background: rgba(15, 23, 42, 0.6);
  border-radius: 1rem;
  overflow: hidden;
  box-shadow: 0 16px 30px rgba(3, 7, 18, 0.35);
  color: var(--text-primary);
  border: 1px solid rgba(148, 163, 184, 0.25);
}

.gallery__item img {
  display: block;
  width: 100%;
  height: 180px;
  object-fit: cover;
}

.gallery__item figcaption {
  margin: 0;
  padding: 0.6rem 0.75rem;
  font-size: 0.85rem;
  color: var(--text-secondary);
  background: rgba(15, 23, 42, 0.55);
}

.modal__footer {
  padding: 1.35rem 1.85rem;
  background: rgba(15, 23, 42, 0.6);
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid rgba(148, 163, 184, 0.2);
}

.contact {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  color: var(--text-secondary);
}

.primary-button {
  padding: 0.75rem 1.6rem;
  border-radius: 0.9rem;
  border: none;
  background: linear-gradient(135deg, var(--accent), rgba(56, 189, 248, 0.85));
  color: var(--text-primary);
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 18px 32px rgba(37, 99, 235, 0.35);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.primary-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 22px 36px rgba(37, 99, 235, 0.45);
}

@media (max-width: 640px) {
  .modal__content {
    padding: 1.25rem;
  }

  .modal__header,
  .modal__footer {
    padding: 1.25rem;
  }

  .gallery__grid {
    grid-template-columns: 1fr;
  }

  .gallery__item img {
    height: 220px;
  }
}
</style>
