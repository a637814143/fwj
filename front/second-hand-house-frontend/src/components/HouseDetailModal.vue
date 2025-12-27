<template>
  <div class="modal-backdrop" @click.self="emit('close')">
    <div class="modal">
      <header class="modal__header">
        <div class="modal__title">
          <h3>{{ house.title }}</h3>
          <p class="modal__address">{{ addressText }}</p>
        </div>
        <button
          type="button"
          class="icon-button"
          :aria-label="t('houseDetail.close')"
          @click="emit('close')"
        >
          ×
        </button>
      </header>

      <section class="modal__content">
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">{{ t('houseDetail.price') }}</span>
            <span class="info-value">{{ formattedPrice }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">{{ t('houseDetail.downPayment') }}</span>
            <span class="info-value">{{ formattedDownPayment }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">{{ t('houseDetail.area') }}</span>
            <span class="info-value">{{ formattedArea }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">{{ t('houseDetail.listedOn') }}</span>
            <span class="info-value">{{ formattedDate }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">{{ t('houseDetail.floor') }}</span>
            <span class="info-value">{{ formattedFloor }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">{{ t('houseDetail.status') }}</span>
            <span class="info-value">
              <span :class="['status-chip', statusClass]">{{ statusLabel }}</span>
            </span>
          </div>
        </div>

        <p v-if="house.description" class="modal__description">{{ house.description }}</p>

        <div v-if="keywordList.length" class="keywords">
          <h4>{{ t('houseDetail.keywords') }}</h4>
          <ul>
            <li v-for="keyword in keywordList" :key="keyword">{{ keyword }}</li>
          </ul>
        </div>

        <div class="gallery">
          <div class="gallery__header">
            <h4>{{ t('houseDetail.gallery') }}</h4>
            <span v-if="images.length" class="gallery__count">{{ galleryCountLabel }}</span>
          </div>
          <p v-if="!images.length" class="gallery__empty">{{ t('houseDetail.galleryEmpty') }}</p>
          <div v-else class="gallery__grid">
            <figure v-for="(image, index) in images" :key="`${index}-${image}`" class="gallery__item">
              <img :src="image" :alt="`${house.title} ${t('houseDetail.galleryAlt', { index: index + 1 })}`" loading="lazy" />
              <figcaption>{{ t('houseDetail.galleryItem', { index: index + 1 }) }}</figcaption>
            </figure>
          </div>
        </div>
      </section>

      <footer class="modal__footer">
        <div class="action-slot">
          <slot name="actions"></slot>
        </div>
        <div class="contact">
          <span>{{ sellerLabel }}</span>
          <span>{{ contactLabel }}</span>
        </div>
        <button type="button" class="primary-button" @click="emit('close')">
          {{ t('houseDetail.close') }}
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { computed, inject } from 'vue';

const props = defineProps({
  house: {
    type: Object,
    required: true
  },
  canViewSensitiveInfo: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['close']);

const translate = inject('translate', (key, vars) => key);
const settings = inject('appSettings', { language: 'zh' });
const t = (key, vars) => translate(key, vars);

const locale = computed(() => (settings?.language === 'en' ? 'en-US' : 'zh-CN'));
const unknownValue = '—';

const images = computed(() => (Array.isArray(props.house?.imageUrls) ? props.house.imageUrls : []));

const addressText = computed(() => {
  const address = props.house?.address;
  if (!address || !String(address).trim()) {
    return t('houseDetail.addressFallback');
  }
  return address;
});

const formatCurrencyValue = (value) => {
  if (value == null || value === '') {
    return unknownValue;
  }
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return unknownValue;
  }
  return new Intl.NumberFormat(locale.value, {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(num);
};

const formatNumberValue = (value, options = {}) => {
  if (value == null || value === '') {
    return unknownValue;
  }
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return unknownValue;
  }
  return new Intl.NumberFormat(locale.value, {
    minimumFractionDigits: options.minimumFractionDigits ?? 0,
    maximumFractionDigits: options.maximumFractionDigits ?? 2
  }).format(num);
};

const formatDateValue = (value) => {
  if (!value) {
    return unknownValue;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return unknownValue;
  }
  return date.toLocaleDateString(locale.value);
};

const formattedPrice = computed(() => formatCurrencyValue(props.house?.price));
const formattedDownPayment = computed(() => formatCurrencyValue(props.house?.downPayment));
const formattedArea = computed(() => {
  const area = formatNumberValue(props.house?.area, { minimumFractionDigits: 0, maximumFractionDigits: 2 });
  return area === unknownValue ? area : t('houseDetail.areaValue', { area });
});
const formattedDate = computed(() => formatDateValue(props.house?.listingDate));
const formattedFloor = computed(() => {
  if (props.house?.floor == null || props.house.floor === '') {
    return unknownValue;
  }
  return t('houseDetail.floorValue', { floor: props.house.floor });
});

const statusLabels = computed(() => {
  const labels = t('statuses');
  return labels && typeof labels === 'object' ? labels : {};
});

const baseStatusLabel = computed(() => {
  const status = props.house?.status;
  return statusLabels.value?.[status] ?? statusLabels.value?.PENDING ?? unknownValue;
});

const baseStatusClass = computed(() => {
  switch (props.house?.status) {
    case 'DRAFT':
      return 'draft';
    case 'APPROVED':
      return 'approved';
    case 'REJECTED':
      return 'rejected';
    case 'SOLD':
      return 'sold';
    default:
      return 'pending';
  }
});

const reservationStatus = computed(() => {
  if (!props.house?.reservationActive) {
    return '';
  }
  return props.house.reservationOwnedByRequester
    ? t('explorer.tips.reservedByYou')
    : t('explorer.tips.reservedByOthers');
});

const statusLabel = computed(() => reservationStatus.value || baseStatusLabel.value);
const statusClass = computed(() => (props.house?.reservationActive ? 'reserved' : baseStatusClass.value));

const keywordList = computed(() => (Array.isArray(props.house?.keywords) ? props.house.keywords : []));
const galleryCountLabel = computed(() => t('houseDetail.galleryCount', { count: images.value.length }));

const sellerName = computed(() => {
  if (props.canViewSensitiveInfo) {
    return props.house?.sellerName ?? unknownValue;
  }
  return maskName(props.house?.sellerName);
});

const maskedPhone = computed(() => {
  const phone = props.house?.contactNumber;
  if (props.canViewSensitiveInfo) {
    return phone ?? unknownValue;
  }
  return maskPhone(phone);
});

const sellerLabel = computed(() => t('houseDetail.seller', { name: sellerName.value }));
const contactLabel = computed(() => t('houseDetail.contact', { phone: maskedPhone.value }));

function maskPhone(value) {
  if (!value) {
    return unknownValue;
  }
  const phone = String(value).trim();
  if (phone.length <= 4) {
    return `${phone}****`;
  }
  const prefix = phone.slice(0, 3);
  const suffix = phone.slice(-2);
  return `${prefix}****${suffix}`;
}

function maskName(value) {
  if (!value) {
    return unknownValue;
  }
  const name = String(value).trim();
  if (name.length <= 1) {
    return `${name}*`;
  }
  return `${name.slice(0, 1)}**`;
}
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(2px);
}

.modal {
  background: var(--gradient-surface);
  border-radius: var(--radius-xl);
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.32);
  max-width: 1040px;
  width: min(96vw, 1040px);
  max-height: 92vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.modal__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 1.4rem 1.8rem 1.2rem;
  gap: 1rem;
}

.modal__title h3 {
  margin: 0;
  color: var(--color-text-strong);
  font-size: 1.45rem;
}

.modal__address {
  margin: 0.35rem 0 0;
  color: var(--color-text-soft);
  font-size: 0.95rem;
}

.icon-button {
  border: none;
  background: rgba(148, 163, 184, 0.18);
  color: var(--color-text-soft);
  border-radius: 50%;
  width: 36px;
  height: 36px;
  font-size: 1.2rem;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background var(--transition-base), color var(--transition-base);
}

.icon-button:hover,
.icon-button:focus-visible {
  background: rgba(37, 99, 235, 0.2);
  color: var(--color-text-strong);
  outline: none;
}

.modal__content {
  padding: 0 1.8rem 1.6rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 1.4rem;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1rem 1.4rem;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.info-label {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.info-value {
  color: var(--color-text-strong);
  font-weight: 600;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  padding: 0.2rem 0.65rem;
  border-radius: var(--radius-pill);
  font-size: 0.85rem;
  background: rgba(148, 163, 184, 0.18);
  color: var(--color-text-strong);
}

.status-chip.approved {
  background: rgba(34, 197, 94, 0.18);
  color: #15803d;
}

.status-chip.rejected {
  background: rgba(248, 113, 113, 0.2);
  color: #b91c1c;
}

.status-chip.reserved {
  background: rgba(250, 204, 21, 0.2);
  color: #854d0e;
}

.status-chip.sold {
  background: rgba(59, 130, 246, 0.18);
  color: #1d4ed8;
}

.modal__description {
  margin: 0;
  color: var(--color-text-strong);
  line-height: 1.6;
}

.keywords h4,
.gallery__header h4 {
  margin: 0 0 0.5rem;
  color: var(--color-text-strong);
}

.keywords ul {
  margin: 0;
  padding-left: 1rem;
  color: var(--color-text-soft);
}

.gallery__header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 1rem;
}

.gallery__count {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.gallery__empty {
  margin: 0;
  padding: 0.85rem 1rem;
  background: rgba(248, 250, 252, 0.75);
  border-radius: var(--radius-md);
  border: 1px dashed rgba(148, 163, 184, 0.4);
  color: var(--color-text-muted);
}

.gallery__grid {
  display: grid;
  gap: 0.9rem;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.gallery__item {
  background: rgba(255, 255, 255, 0.78);
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
}

.gallery__item img {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.gallery__item figcaption {
  margin: 0;
  padding: 0.6rem 0.8rem;
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

.modal__footer {
  padding: 1.2rem 1.8rem 1.6rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  border-top: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.65);
}

.action-slot {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.action-slot .primary,
.action-slot .secondary,
.action-slot .ghost {
  border-radius: var(--radius-pill);
  padding: 0.5rem 1.2rem;
  border: 1px solid color-mix(in srgb, var(--color-border, rgba(148, 163, 184, 0.4)) 85%, transparent);
  background: color-mix(in srgb, var(--color-surface, #fff) 90%, transparent);
  color: var(--color-text-strong, #0f172a);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.action-slot .primary {
  background: var(--gradient-primary, linear-gradient(135deg, #5ddcff, #7f6bff));
  color: var(--color-text-on-emphasis, #0b1220);
  box-shadow: 0 18px 36px rgba(93, 220, 255, 0.28);
  border: none;
}

.action-slot .secondary {
  background: color-mix(in srgb, var(--color-accent, #5ddcff) 12%, transparent);
  color: var(--color-text-strong, #0f172a);
}

.action-slot .ghost {
  background: transparent;
}

.action-slot button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

.action-slot button:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 16px 28px rgba(127, 107, 255, 0.24);
}

.contact {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  color: var(--color-text-soft);
}

.primary-button {
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.65rem 1.6rem;
  background: var(--color-primary);
  color: var(--color-text-on-emphasis);
  font-weight: 600;
  cursor: pointer;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.primary-button:hover,
.primary-button:focus-visible {
  transform: translateY(-1px);
  box-shadow: 0 16px 28px rgba(37, 99, 235, 0.25);
  outline: none;
}

@media (max-width: 640px) {
  .modal {
    width: 94vw;
  }

  .info-grid {
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  }

  .modal__footer {
    flex-direction: column;
    align-items: stretch;
  }

  .contact {
    align-items: flex-start;
  }
}
</style>
