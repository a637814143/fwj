<template>
  <section class="order-history">
    <h2>房屋订单</h2>
    <p v-if="!currentUser" class="hint">请登录后查看订单记录与退换流程。</p>

    <div v-else>
      <div v-if="loading" class="loading">订单数据加载中...</div>
      <div v-else-if="!orders || orders.length === 0" class="empty">暂无订单记录。</div>
      <ul v-else class="order-list">
        <li v-for="order in orders" :key="order.id" class="order-card">
          <header>
            <div class="title-area">
              <h3>{{ order.houseTitle }}</h3>
              <span v-if="stageLabel(order.progressStage)" class="progress-chip">
                {{ stageLabel(order.progressStage) }}
              </span>
            </div>
            <span class="status" :class="order.status.toLowerCase()">
              {{ statusLabels[order.status] ?? order.status }}
            </span>
          </header>
          <dl>
            <div>
              <dt>订单编号</dt>
              <dd>{{ order.id }}</dd>
            </div>
            <div>
              <dt>金额（元）</dt>
              <dd>{{ formatAmount(order.amount) }}</dd>
            </div>
            <div>
              <dt>支付方式</dt>
              <dd>{{ paymentMethodLabel(order.paymentMethod) }}</dd>
            </div>
            <div>
              <dt>买家</dt>
              <dd>{{ buyerDisplayName(order) }}（{{ buyerUsernameDisplay(order) }}）</dd>
            </div>
            <div>
              <dt>卖家</dt>
              <dd>{{ sellerDisplayName(order) }}（{{ sellerUsernameDisplay(order) }}）</dd>
            </div>
            <div>
              <dt>创建时间</dt>
              <dd>{{ formatTime(order.createdAt) }}</dd>
            </div>
            <div>
              <dt>更新时间</dt>
              <dd>{{ formatTime(order.updatedAt) }}</dd>
            </div>
            <div v-if="order.returnReason">
              <dt>退换原因</dt>
              <dd>{{ order.returnReason }}</dd>
            </div>
          </dl>
          <section class="progress-tracker">
            <h4>交易进度</h4>
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
              <h4>预约看房</h4>
              <span v-if="order.viewingTime" class="viewing-time">{{ viewingTimeLabel(order) }}</span>
            </header>
            <p v-if="order.viewingTime" class="viewing-summary">
              预约时间：{{ viewingTimeLabel(order) }}
              <span v-if="order.viewingMessage">备注：{{ order.viewingMessage }}</span>
            </p>
            <div class="viewing-actions">
              <button
                v-if="canSchedule(order)"
                type="button"
                class="primary"
                :disabled="loading"
                @click="openScheduleDialog(order)"
              >
                选择看房时间
              </button>
              <button
                v-if="canAdvance(order)"
                type="button"
                class="secondary"
                :disabled="loading"
                @click="advanceProgress(order)"
              >
                推进至{{ nextStageLabel(order) }}
              </button>
              <button
                v-if="canConfirmViewing(order)"
                type="button"
                class="ghost"
                :disabled="loading"
                @click="confirmViewing(order)"
              >
                确认预约
              </button>
            </div>
          </section>
          <footer v-if="canRequestReturn(order)" class="actions">
            <button type="button" :disabled="loading" @click="requestReturn(order)">
              申请退换
            </button>
          </footer>
        </li>
      </ul>
      <transition name="fade">
        <div v-if="scheduleState.visible" class="schedule-overlay" @click.self="closeScheduleDialog">
          <div class="schedule-dialog">
            <header>
              <h3>安排看房</h3>
              <p>{{ scheduleState.title }}</p>
            </header>
            <form @submit.prevent="submitSchedule">
              <label>
                <span>看房时间</span>
                <input v-model="scheduleState.time" type="datetime-local" required />
              </label>
              <label>
                <span>备注（可选）</span>
                <textarea
                  v-model="scheduleState.message"
                  rows="2"
                  maxlength="120"
                  placeholder="给买家的补充说明"
                ></textarea>
              </label>
              <p v-if="scheduleError" class="form-error">{{ scheduleError }}</p>
              <div class="dialog-actions">
                <button type="button" class="ghost" @click="closeScheduleDialog">取消</button>
                <button type="submit" class="primary" :disabled="loading">确认安排</button>
              </div>
            </form>
          </div>
        </div>
      </transition>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from 'vue';

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

const statusLabels = {
  PENDING: '待支付',
  RESERVED: '已预定',
  PAID: '已支付',
  RETURNED: '已退回',
  CANCELLED: '已取消'
};

const paymentMethodLabels = {
  FULL: '全款支付',
  INSTALLMENT: '分期付款'
};

const progressOrderDefault = ['DEPOSIT_PAID', 'VIEWING_SCHEDULED', 'FEEDBACK_SUBMITTED', 'HANDOVER_COMPLETED'];

const progressOrderNormalized = computed(() => {
  if (Array.isArray(props.progressOrder) && props.progressOrder.length > 0) {
    return props.progressOrder;
  }
  return progressOrderDefault;
});

const stageLabel = (stage) => props.progressLabels?.[stage] ?? stage ?? '';

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
    props.currentUser &&
    order.buyerUsername === props.currentUser.username
  );
};

const requestReturn = (order) => {
  if (!canRequestReturn(order) || props.loading) {
    return;
  }
  const reason = window.prompt('请输入退换原因（可选）', order.returnReason ?? '');
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
  const yuan = num * 10000;
  return yuan.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

const paymentMethodLabel = (value) => paymentMethodLabels[value] ?? '—';

const maskName = (value) => {
  if (!value) {
    return '—';
  }
  const text = String(value);
  if (text.length === 1) {
    return `${text}*`;
  }
  return `${text.slice(0, 1)}**`;
};

const maskUsername = (value) => {
  if (!value) {
    return '—';
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

const buyerDisplayName = (order) => {
  const name = order?.buyerDisplayName ?? '—';
  return shouldMaskBuyer(order) ? maskName(name) : name;
};

const buyerUsernameDisplay = (order) => {
  const username = order?.buyerUsername ?? '—';
  return shouldMaskBuyer(order) ? maskUsername(username) : username;
};

const sellerDisplayName = (order) => {
  const name = order?.sellerDisplayName ?? '—';
  return shouldMaskSeller(order) ? maskName(name) : name;
};

const sellerUsernameDisplay = (order) => {
  const username = order?.sellerUsername ?? '—';
  return shouldMaskSeller(order) ? maskUsername(username) : username;
};

const formatTime = (value) => {
  if (!value) {
    return '';
  }
  return new Date(value).toLocaleString('zh-CN', { hour12: false });
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
  scheduleState.title = order.houseTitle ?? '';
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
    scheduleError.value = '请选择预约时间';
    return;
  }
  emit('schedule-viewing', {
    orderId: scheduleState.orderId,
    viewingTime: scheduleState.time,
    message: scheduleState.message
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
  return Boolean(nextStage(order));
};

const canConfirmViewing = (order) => {
  if (!order || !isBuyer.value || props.loading) {
    return false;
  }
  if (props.currentUser?.username !== order.buyerUsername) {
    return false;
  }
  return order.progressStage === 'VIEWING_SCHEDULED';
};

const viewingTimeLabel = (order) => {
  if (!order?.viewingTime) {
    return '';
  }
  return formatTime(order.viewingTime);
};

const hasViewingSection = (order) => {
  return (
    Boolean(order?.viewingTime) ||
    canSchedule(order) ||
    canAdvance(order) ||
    canConfirmViewing(order)
  );
};

const advanceProgress = (order) => {
  const target = nextStage(order);
  if (!target || props.loading) {
    return;
  }
  emit('advance-progress', { orderId: order.id, stage: target });
};

const confirmViewing = (order) => {
  if (!order || props.loading) {
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
