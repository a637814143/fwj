<template>
  <section class="order-history">
    <h2>{{ t('orders.history.title') }}</h2>
    <p v-if="!currentUser" class="hint">{{ t('orders.history.hint') }}</p>

    <div v-else>
      <div v-if="loading" class="loading">{{ t('orders.history.loading') }}</div>
      <div v-else-if="!orders || orders.length === 0" class="empty">{{ t('orders.history.empty') }}</div>
      <ul v-else class="order-list">
        <li v-for="order in orders" :key="order.id" class="order-card">
          <header>
            <div class="title-area">
              <h3>{{ order.houseTitle }}</h3>
              <span v-if="stageLabel(order.progressStage)" class="progress-chip">
                {{ stageLabel(order.progressStage) }}
              </span>
            </div>
            <span class="status" :class="statusClass(order.status)">
              {{ statusLabel(order.status) }}
            </span>
          </header>
          <dl>
            <div>
              <dt>{{ t('orders.history.fields.orderId') }}</dt>
              <dd>{{ order.id }}</dd>
            </div>
            <div>
              <dt>{{ t('orders.history.fields.amount') }}</dt>
              <dd>￥{{ formatAmount(order.amount) }}</dd>
            </div>
            <div>
              <dt>{{ t('orders.history.fields.paymentMethod') }}</dt>
              <dd>{{ paymentMethodLabel(order.paymentMethod) }}</dd>
            </div>
            <div>
              <dt>{{ t('orders.history.fields.buyer') }}</dt>
              <dd>{{ buyerDisplayName(order) }} ({{ buyerUsernameDisplay(order) }})</dd>
            </div>
            <div>
              <dt>{{ t('orders.history.fields.seller') }}</dt>
              <dd>{{ sellerDisplayName(order) }} ({{ sellerUsernameDisplay(order) }})</dd>
            </div>
            <div>
              <dt>{{ t('orders.history.fields.createdAt') }}</dt>
              <dd>{{ formatTime(order.createdAt) }}</dd>
            </div>
            <div>
              <dt>{{ t('orders.history.fields.updatedAt') }}</dt>
              <dd>{{ formatTime(order.updatedAt) }}</dd>
            </div>
            <div>
              <dt>{{ t('orders.history.fields.escrowStatus') }}</dt>
              <dd>
                <span :class="['escrow-tag', escrowClass(order)]">{{ escrowStatus(order) }}</span>
                <span v-if="escrowSupplement(order)" class="escrow-supplement">{{ escrowSupplement(order) }}</span>
              </dd>
            </div>
            <div v-if="shouldShowPlatformFee(order)">
              <dt>{{ t('orders.history.fields.platformFee') }}</dt>
              <dd>￥{{ formatAmount(order.platformFee) }}</dd>
            </div>
            <div v-if="shouldShowReleasedAmount(order)">
              <dt>{{ t('orders.history.fields.releasedAmount') }}</dt>
              <dd>￥{{ formatAmount(order.releasedAmount) }}</dd>
            </div>
            <div v-if="order.adminReviewedBy">
              <dt>{{ t('orders.history.fields.reviewedBy') }}</dt>
              <dd>
                {{ order.adminReviewedBy }}
                <span v-if="order.adminReviewedAt" class="escrow-supplement">{{ formatTime(order.adminReviewedAt) }}</span>
              </dd>
            </div>
            <div v-if="order.returnReason">
              <dt>{{ t('orders.history.fields.returnReason') }}</dt>
              <dd>{{ order.returnReason }}</dd>
            </div>
          </dl>
          <p v-if="isSellerViewer(order) && hasSellerRepay(order)" class="seller-repay-alert">
            {{ sellerRepayPending(order) }}
          </p>
          <p v-else-if="isSellerViewer(order) && order.sellerRepaySettledAt" class="seller-repay-info">
            {{ sellerRepayCompleted(order) }}
          </p>
          <section class="progress-tracker">
            <h4>{{ t('orders.history.progressTitle') }}</h4>
            <ol class="progress-steps">
              <li
                v-for="(stage, index) in progressOrderNormalized"
                :key="`${order.id}-${stage}`"
                :class="['progress-step', { reached: isStageReached(order, stage), current: isCurrentStage(order, stage) }]"
              >
                <span class="step-index">{{ index + 1 }}</span>
                <span class="step-label">{{ stageLabel(stage) }}</span>
              </li>
            </ol>
          </section>
          <section v-if="hasViewingSection(order)" class="viewing-section">
            <header>
              <h4>{{ t('orders.history.viewing.title') }}</h4>
              <span v-if="order.viewingTime" class="viewing-time">
                {{ t('orders.history.viewing.timeLabel', { time: viewingTimeLabel(order) }) }}
              </span>
            </header>
            <p v-if="order.viewingTime" class="viewing-summary">
              {{ t('orders.history.viewing.appointment', { time: viewingTimeLabel(order) }) }}
              <span v-if="order.viewingMessage">
                {{ t('orders.history.viewing.notes', { message: order.viewingMessage }) }}
              </span>
            </p>
            <div
              v-if="order.buyerViewingConfirmed || order.sellerViewingConfirmed"
              class="viewing-status"
            >
              <span v-if="order.buyerViewingConfirmed" class="viewing-chip">
                {{ t('orders.history.viewing.confirmedBuyer') }}
              </span>
              <span v-if="order.sellerViewingConfirmed" class="viewing-chip">
                {{ t('orders.history.viewing.confirmedSeller') }}
              </span>
            </div>
            <div class="viewing-actions">
              <button
                v-if="canSchedule(order)"
                type="button"
                class="primary"
                :disabled="loading"
                @click="openScheduleDialog(order)"
              >
                {{ t('orders.history.viewing.schedule') }}
              </button>
              <button
                v-if="canAdvance(order)"
                type="button"
                class="secondary"
                :disabled="loading"
                @click="advanceProgress(order)"
              >
                {{ t('orders.history.viewing.advance', { stage: nextStageLabel(order) }) }}
              </button>
              <button
                v-if="canConfirmViewing(order)"
                type="button"
                class="secondary"
                :disabled="loading"
                @click="confirmViewing(order)"
              >
                {{ t('orders.history.viewing.markViewed') }}
              </button>
            </div>
          </section>
          <footer v-if="canRequestReturn(order)" class="actions">
            <button type="button" :disabled="loading" @click="requestReturn(order)">
              {{ t('orders.history.actions.requestReturn') }}
            </button>
          </footer>
        </li>
      </ul>
      <transition name="fade">
        <div v-if="scheduleState.visible" class="schedule-overlay" @click.self="closeScheduleDialog">
          <div class="schedule-dialog">
            <header>
              <h3>{{ t('orders.history.schedule.dialogTitle') }}</h3>
              <p>{{ scheduleState.title }}</p>
            </header>
            <form @submit.prevent="submitSchedule">
              <label>
                <span>{{ t('orders.history.schedule.time') }}</span>
                <input v-model="scheduleState.time" type="datetime-local" required />
              </label>
              <label>
                <span>{{ t('orders.history.schedule.notes') }}</span>
                <textarea
                  v-model="scheduleState.message"
                  rows="2"
                  maxlength="120"
                  :placeholder="t('orders.history.schedule.notesPlaceholder')"
                ></textarea>
              </label>
              <p v-if="scheduleError" class="form-error">{{ scheduleError }}</p>
              <div class="dialog-actions">
                <button type="button" class="ghost" @click="closeScheduleDialog">
                  {{ t('orders.history.schedule.cancel') }}
                </button>
                <button type="submit" class="primary" :disabled="loading">
                  {{ t('orders.history.schedule.confirm') }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </transition>
    </div>
  </section>
</template>

<script setup>
import { computed, inject, reactive, ref } from 'vue';

const props = defineProps({
  orders: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  currentUser: {
    type: Object,
    default: null
  },
  canViewSensitiveInfo: {
    type: Boolean,
    default: false
  },
  progressLabels: {
    type: Object,
    default: () => ({})
  },
  progressOrder: {
    type: Array,
    default: () => ['DEPOSIT_PAID', 'VIEWING_SCHEDULED', 'FEEDBACK_SUBMITTED', 'HANDOVER_COMPLETED']
  }
});

const emit = defineEmits(['request-return', 'schedule-viewing', 'advance-progress', 'confirm-viewing']);

const settings = inject('appSettings', { language: 'zh' });
const translate = inject('translate', (key, vars) => key);
const t = (key, vars) => translate(key, vars);

const locale = computed(() => (settings?.language === 'en' ? 'en-US' : 'zh-CN'));

const historyTexts = computed(() => {
  const value = t('orders.history');
  return value && typeof value === 'object' ? value : {};
});

const fallbackText = computed(() => historyTexts.value.noData ?? '—');

const statusLabels = computed(() => historyTexts.value.status ?? {});

const paymentMethodLabels = computed(() => historyTexts.value.paymentMethods ?? {});

const escrowTexts = computed(() => historyTexts.value.escrow ?? {});

const scheduleTexts = computed(() => historyTexts.value.schedule ?? {});

const viewingTexts = computed(() => historyTexts.value.viewing ?? {});

const promptTexts = computed(() => historyTexts.value.prompts ?? {});

const progressOrderDefault = [
  'DEPOSIT_PAID',
  'VIEWING_SCHEDULED',
  'FEEDBACK_SUBMITTED',
  'HANDOVER_COMPLETED',
  'FUNDS_RELEASED'
];

const normalizeKey = (value) => {
  if (value == null) {
    return '';
  }
  const text = String(value).trim();
  return text ? text.toUpperCase() : '';
};

const statusLabel = (value) => {
  const key = normalizeKey(value);
  if (!key) {
    return fallbackText.value;
  }
  const map = statusLabels.value ?? {};
  return map[key] ?? map[value] ?? map[key.replace(/\s+/g, '_')] ?? fallbackText.value;
};

const statusClass = (value) => {
  const key = String(value ?? '').trim();
  if (!key) {
    return 'unknown';
  }
  return key.toLowerCase().replace(/[^a-z0-9]+/g, '-');
};

const paymentMethodLabel = (value) => {
  const key = normalizeKey(value);
  if (!key) {
    return fallbackText.value;
  }
  const map = paymentMethodLabels.value ?? {};
  return map[key] ?? map[value] ?? fallbackText.value;
};

const progressLocaleLabels = computed(() => {
  const labels = t('orders.progress');
  return labels && typeof labels === 'object' ? labels : {};
});

const progressOrderNormalized = computed(() => {
  if (Array.isArray(props.progressOrder) && props.progressOrder.length > 0) {
    return props.progressOrder;
  }
  return progressOrderDefault;
});

const stageLabel = (stage) =>
  props.progressLabels?.[stage] ?? progressLocaleLabels.value?.[stage] ?? stage ?? '';

const stageIndex = (stage) => progressOrderNormalized.value.indexOf(stage);

const isStageReached = (order, stage) => {
  if (!order) {
    return false;
  }
  const currentIndex = stageIndex(order.progressStage);
  const targetIndex = stageIndex(stage);
  if (currentIndex < 0 || targetIndex < 0) {
    return false;
  }
  return currentIndex >= targetIndex;
};

const isCurrentStage = (order, stage) => order?.progressStage === stage;

const nextStage = (order) => {
  const index = stageIndex(order?.progressStage);
  if (index < 0) {
    return null;
  }
  return progressOrderNormalized.value[index + 1] ?? null;
};

const nextStageLabel = (order) => {
  const stage = nextStage(order);
  return stage ? stageLabel(stage) : '';
};

const sellerRoles = ['SELLER', 'LANDLORD'];
const isSeller = computed(() => sellerRoles.includes(props.currentUser?.role));
const isBuyer = computed(() => props.currentUser?.role === 'BUYER');

const scheduleState = reactive({
  visible: false,
  orderId: null,
  title: '',
  time: '',
  message: ''
});

const scheduleError = ref('');

const canRequestReturn = (order) => {
  if (!order) {
    return false;
  }
  return (
    order.status === 'PAID' &&
    order.progressStage === 'FUNDS_RELEASED' &&
    props.currentUser &&
    order.buyerUsername === props.currentUser.username
  );
};

const requestReturn = (order) => {
  if (!canRequestReturn(order) || props.loading) {
    return;
  }
  const reason = window.prompt(
    promptTexts.value.returnReason ?? '',
    order.returnReason ?? ''
  );
  if (reason === null) {
    return;
  }
  emit('request-return', { orderId: order.id, reason: reason.trim() });
};

const formatAmount = (value) => {
  if (value == null) {
    return '0.00';
  }
  const num = Number(value);
  if (!Number.isFinite(num)) {
    return '0.00';
  }
  return num.toLocaleString(locale.value, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

const toNumberSafe = (value) => {
  const num = Number(value);
  return Number.isFinite(num) ? num : 0;
};

const escrowStatus = (order) => {
  if (!order) {
    return fallbackText.value;
  }
  if (order.adminReviewed) {
    if (order.fundsReleasedTo === 'SELLER') {
      return escrowTexts.value.releasedSeller ?? fallbackText.value;
    }
    if (order.fundsReleasedTo === 'BUYER') {
      return escrowTexts.value.releasedBuyer ?? fallbackText.value;
    }
    return escrowTexts.value.settled ?? fallbackText.value;
  }
  return escrowTexts.value.awaiting ?? fallbackText.value;
};

const escrowClass = (order) => {
  if (!order || !order.adminReviewed) {
    return 'pending';
  }
  if (order.fundsReleasedTo === 'BUYER') {
    return 'warning';
  }
  return 'success';
};

const escrowSupplement = (order) => {
  if (!order) {
    return '';
  }
  if (!order.adminReviewed) {
    const hold = order.adminHoldAmount ?? order.amount;
    if (hold == null) {
      return '';
    }
    return t('orders.history.escrow.hold', { amount: formatAmount(hold) });
  }
  const released = toNumberSafe(order.releasedAmount);
  if (released > 0) {
    return t('orders.history.escrow.released', {
      amount: formatAmount(order.releasedAmount)
    });
  }
  return '';
};

const shouldShowPlatformFee = (order) => toNumberSafe(order?.platformFee) > 0;

const shouldShowReleasedAmount = (order) => toNumberSafe(order?.releasedAmount) > 0;

const hasSellerRepay = (order) => Boolean(order?.sellerRepayRequired);

const sellerRepayPending = (order) => {
  if (!order) {
    return '';
  }
  return t('orders.history.repayment.pending', {
    amount: formatAmount(order.sellerRepayAmount)
  });
};

const sellerRepayCompleted = (order) => {
  if (!order?.sellerRepaySettledAt) {
    return '';
  }
  return t('orders.history.repayment.completed', {
    time: formatTime(order.sellerRepaySettledAt)
  });
};

const maskName = (value) => {
  if (!value) {
    return fallbackText.value;
  }
  const text = String(value);
  if (text.length === 1) {
    return `${text}*`;
  }
  return `${text.slice(0, 1)}**`;
};

const maskUsername = (value) => {
  if (!value) {
    return fallbackText.value;
  }
  const text = String(value);
  if (text.length <= 2) {
    return '*'.repeat(text.length);
  }
  return `${text.slice(0, 1)}${'*'.repeat(text.length - 2)}${text.slice(-1)}`;
};

const shouldMaskBuyer = (order) => {
  if (!order) {
    return true;
  }
  if (props.currentUser && order.buyerUsername === props.currentUser.username) {
    return false;
  }
  return !props.canViewSensitiveInfo;
};

const shouldMaskSeller = (order) => {
  if (!order) {
    return true;
  }
  if (props.currentUser && order.sellerUsername === props.currentUser.username) {
    return false;
  }
  return !props.canViewSensitiveInfo;
};

const isSellerViewer = (order) => {
  if (!order || !props.currentUser) {
    return false;
  }
  return order.sellerUsername === props.currentUser.username;
};

const buyerDisplayName = (order) => {
  const name = order?.buyerDisplayName ?? fallbackText.value;
  return shouldMaskBuyer(order) ? maskName(name) : name;
};

const buyerUsernameDisplay = (order) => {
  const username = order?.buyerUsername ?? fallbackText.value;
  return shouldMaskBuyer(order) ? maskUsername(username) : username;
};

const sellerDisplayName = (order) => {
  const name = order?.sellerDisplayName ?? fallbackText.value;
  return shouldMaskSeller(order) ? maskName(name) : name;
};

const sellerUsernameDisplay = (order) => {
  const username = order?.sellerUsername ?? fallbackText.value;
  return shouldMaskSeller(order) ? maskUsername(username) : username;
};

const formatTime = (value) => {
  if (!value) {
    return '';
  }
  return new Date(value).toLocaleString(locale.value, { hour12: false });
};

const toLocalInputValue = (value) => {
  if (!value) {
    return '';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  const offset = date.getTimezoneOffset();
  const local = new Date(date.getTime() - offset * 60000);
  return local.toISOString().slice(0, 16);
};

const openScheduleDialog = (order) => {
  if (!order) {
    return;
  }
  scheduleState.visible = true;
  scheduleState.orderId = order.id;
  scheduleState.title = t('orders.history.schedule.subtitle', {
    title: order.houseTitle ?? ''
  });
  scheduleState.time = toLocalInputValue(order.viewingTime);
  scheduleState.message = order.viewingMessage ?? '';
  scheduleError.value = '';
};

const closeScheduleDialog = () => {
  scheduleState.visible = false;
  scheduleState.orderId = null;
  scheduleState.title = '';
  scheduleState.time = '';
  scheduleState.message = '';
  scheduleError.value = '';
};

const submitSchedule = () => {
  if (!scheduleState.orderId) {
    return;
  }
  if (!scheduleState.time) {
    scheduleError.value = scheduleTexts.value.validation ?? '';
    return;
  }
  emit('schedule-viewing', {
    orderId: scheduleState.orderId,
    viewingTime: scheduleState.time,
    message: scheduleState.message?.trim()
  });
  closeScheduleDialog();
};

const canSchedule = (order) => {
  if (!order || !isSeller.value || props.loading) {
    return false;
  }
  if (props.currentUser?.username !== order.sellerUsername) {
    return false;
  }
  const firstStage = progressOrderNormalized.value[0];
  return order.progressStage === firstStage;
};

const canAdvance = (order) => {
  if (!order || !isSeller.value || props.loading) {
    return false;
  }
  if (props.currentUser?.username !== order.sellerUsername) {
    return false;
  }
  const firstStage = progressOrderNormalized.value[0];
  if (order.progressStage === firstStage) {
    return false;
  }
  const targetStage = nextStage(order);
  if (!targetStage || targetStage === 'FUNDS_RELEASED') {
    return false;
  }
  return true;
};

const canBuyerConfirmViewing = (order) => {
  if (!order || !isBuyer.value || props.loading) {
    return false;
  }
  if (props.currentUser?.username !== order.buyerUsername) {
    return false;
  }
  return order.progressStage === 'VIEWING_SCHEDULED' && !order.buyerViewingConfirmed;
};

const canSellerConfirmViewing = (order) => {
  if (!order || !isSeller.value || props.loading) {
    return false;
  }
  if (props.currentUser?.username !== order.sellerUsername) {
    return false;
  }
  return order.progressStage === 'VIEWING_SCHEDULED' && !order.sellerViewingConfirmed;
};

const canConfirmViewing = (order) => canBuyerConfirmViewing(order) || canSellerConfirmViewing(order);

const viewingTimeLabel = (order) => {
  if (!order?.viewingTime) {
    return '';
  }
  return formatTime(order.viewingTime);
};

const hasViewingSection = (order) =>
  Boolean(order?.viewingTime) ||
  canSchedule(order) ||
  canAdvance(order) ||
  canConfirmViewing(order) ||
  Boolean(order?.buyerViewingConfirmed) ||
  Boolean(order?.sellerViewingConfirmed);

const advanceProgress = (order) => {
  const target = nextStage(order);
  if (!target || target === 'FUNDS_RELEASED' || props.loading) {
    return;
  }
  emit('advance-progress', { orderId: order.id, stage: target });
};

const confirmViewing = (order) => {
  if (!order || props.loading || !canConfirmViewing(order)) {
    return;
  }
  emit('confirm-viewing', { orderId: order.id });
};
</script>

<style scoped>
.order-history {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
  background: var(--gradient-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  padding: 1.85rem;
  border: 1px solid var(--color-border);
  backdrop-filter: blur(var(--glass-blur));
}

.order-history h2 {
  margin: 0;
  font-size: 1.55rem;
  color: var(--color-text-strong);
}

.hint {
  margin: 0;
  color: var(--color-text-soft);
}

.loading,
.empty {
  background: rgba(248, 250, 252, 0.85);
  border-radius: var(--radius-md);
  padding: 1.1rem 1.35rem;
  color: var(--color-text-muted);
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.order-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
}

.order-card {
  border: 1px solid rgba(226, 232, 240, 0.65);
  border-radius: var(--radius-lg);
  padding: 1.35rem;
  display: flex;
  flex-direction: column;
  gap: 1.05rem;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.9));
  box-shadow: 0 22px 48px rgba(15, 23, 42, 0.12);
}

.order-card header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.title-area {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.progress-chip {
  display: inline-flex;
  align-items: center;
  padding: 0.2rem 0.8rem;
  background: rgba(59, 130, 246, 0.16);
  color: #1d4ed8;
  border-radius: var(--radius-pill);
  font-size: 0.85rem;
  font-weight: 600;
}

.order-card h3 {
  margin: 0;
  color: var(--color-text-strong);
}

.status {
  border-radius: var(--radius-pill);
  padding: 0.35rem 0.85rem;
  font-size: 0.88rem;
  font-weight: 600;
}

.status.paid {
  background: rgba(34, 197, 94, 0.2);
  color: #15803d;
}

.status.pending {
  background: rgba(59, 130, 246, 0.2);
  color: #1d4ed8;
}

.status.returned {
  background: rgba(248, 113, 113, 0.22);
  color: #b91c1c;
}

.status.reserved {
  background: rgba(250, 204, 21, 0.22);
  color: #b45309;
}

.status.cancelled {
  background: rgba(148, 163, 184, 0.28);
  color: #475569;
}

.escrow-tag {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.6rem;
  border-radius: var(--radius-pill);
  font-size: 0.85rem;
  font-weight: 600;
}

.escrow-tag.pending {
  background: rgba(59, 130, 246, 0.16);
  color: #1d4ed8;
}

.escrow-tag.success {
  background: rgba(34, 197, 94, 0.2);
  color: #15803d;
}

.escrow-tag.warning {
  background: rgba(245, 158, 11, 0.18);
  color: #92400e;
}

.seller-repay-alert {
  margin: 0.9rem 0 0;
  padding: 0.85rem 1rem;
  border-radius: var(--radius-md);
  background: rgba(235, 202, 195, 0.6);
  border: 1px solid rgba(222, 180, 170, 0.45);
  color: #b4534b;
  font-weight: 600;
}

.seller-repay-info {
  margin: 0.75rem 0 0;
  padding: 0.75rem 1rem;
  border-radius: var(--radius-md);
  background: rgba(204, 236, 228, 0.5);
  border: 1px solid rgba(150, 210, 200, 0.4);
  color: #276764;
  font-weight: 500;
}

.escrow-supplement {
  display: block;
  margin-top: 0.35rem;
  color: var(--color-text-soft);
  font-size: 0.85rem;
}

dl {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 0.8rem 1.35rem;
  margin: 0;
}

dl div {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

dl dt {
  color: var(--color-text-soft);
  font-size: 0.9rem;
  font-weight: 600;
}

.progress-tracker {
  margin-top: 1.1rem;
  padding-top: 1rem;
  border-top: 1px dashed rgba(148, 163, 184, 0.35);
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.progress-tracker h4 {
  margin: 0;
  font-size: 1rem;
  color: var(--color-text-muted);
}

.progress-steps {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 0.8rem;
  list-style: none;
  margin: 0;
  padding: 0;
}

.progress-step {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.75rem 0.85rem;
  border-radius: var(--radius-md);
  border: 1px dashed rgba(148, 163, 184, 0.45);
  background: rgba(255, 255, 255, 0.65);
  transition: background var(--transition-base), border-color var(--transition-base),
    box-shadow var(--transition-base);
}

.progress-step.reached {
  border-color: rgba(37, 99, 235, 0.45);
  background: rgba(59, 130, 246, 0.12);
  box-shadow: 0 8px 18px rgba(59, 130, 246, 0.12);
}

.progress-step.current {
  border-color: rgba(37, 99, 235, 0.65);
  background: rgba(59, 130, 246, 0.2);
}

.step-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--gradient-primary);
  color: #fff;
  font-size: 0.85rem;
  font-weight: 600;
}

.step-label {
  font-size: 0.92rem;
  color: var(--color-text-strong);
  font-weight: 600;
}

.viewing-section {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
  margin-top: 1rem;
  padding: 1rem 1.05rem;
  border-radius: var(--radius-md);
  background: rgba(248, 250, 252, 0.85);
  border: 1px solid rgba(148, 163, 184, 0.35);
}

.viewing-section header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.85rem;
}

.viewing-section h4 {
  margin: 0;
  font-size: 1rem;
  color: var(--color-text-muted);
}

.viewing-time {
  font-size: 0.9rem;
  color: var(--color-text-soft);
}

.viewing-summary {
  margin: 0;
  font-size: 0.95rem;
  color: var(--color-text-strong);
}

.viewing-status {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.viewing-chip {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.75rem;
  border-radius: var(--radius-pill);
  background: rgba(34, 197, 94, 0.16);
  color: #15803d;
  font-size: 0.85rem;
  font-weight: 600;
}

.viewing-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.viewing-actions button {
  border-radius: var(--radius-pill);
  padding: 0.45rem 1.2rem;
  border: none;
  font-weight: 600;
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.viewing-actions .primary {
  background: var(--gradient-primary);
  color: #fff;
  box-shadow: 0 16px 32px rgba(59, 130, 246, 0.25);
}

.viewing-actions .secondary {
  background: rgba(59, 130, 246, 0.14);
  color: #1d4ed8;
}

.viewing-actions .ghost {
  background: transparent;
  color: var(--color-text-muted);
  border: 1px solid rgba(148, 163, 184, 0.4);
}

.viewing-actions button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.schedule-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 40;
  padding: 1.5rem;
}

.schedule-dialog {
  background: var(--gradient-surface);
  border-radius: var(--radius-lg);
  padding: 1.8rem;
  width: min(420px, 100%);
  box-shadow: var(--shadow-lg);
  border: 1px solid rgba(148, 163, 184, 0.32);
}

.schedule-dialog header {
  margin-bottom: 1.2rem;
}

.schedule-dialog h3 {
  margin: 0 0 0.3rem;
  font-size: 1.2rem;
  color: var(--color-text-strong);
}

.schedule-dialog p {
  margin: 0;
  color: var(--color-text-soft);
  font-size: 0.95rem;
}

.schedule-dialog form {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.schedule-dialog label {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  font-weight: 600;
  color: var(--color-text-muted);
}

.schedule-dialog input,
.schedule-dialog textarea {
  width: 100%;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 0.5rem;
}

.dialog-actions .primary {
  background: var(--gradient-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.5rem 1.35rem;
}

.dialog-actions .ghost {
  background: transparent;
  color: var(--color-text-muted);
  border: 1px solid rgba(148, 163, 184, 0.45);
  border-radius: var(--radius-pill);
  padding: 0.5rem 1.35rem;
}

.form-error {
  margin: 0;
  color: #b91c1c;
  font-size: 0.85rem;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

dl dd {
  margin: 0;
  color: var(--color-text-strong);
  font-size: 0.97rem;
}

.actions {
  display: flex;
  justify-content: flex-end;
}

.actions button {
  background: linear-gradient(135deg, #f97316, #ea580c);
  border: none;
  border-radius: var(--radius-pill);
  color: #fff;
  cursor: pointer;
  font-weight: 600;
  padding: 0.6rem 1.5rem;
  box-shadow: 0 16px 32px rgba(234, 88, 12, 0.28);
  transition: transform var(--transition-base), box-shadow var(--transition-base),
    background var(--transition-base);
}

.actions button:hover {
  transform: translateY(-2px);
  box-shadow: 0 24px 45px rgba(234, 88, 12, 0.32);
}

.actions button:disabled {
  background: rgba(253, 186, 116, 0.8);
  cursor: not-allowed;
  box-shadow: none;
}
</style>
