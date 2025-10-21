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
            <h3>{{ order.houseTitle }}</h3>
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
          <footer v-if="canRequestReturn(order)" class="actions">
            <button type="button" :disabled="loading" @click="requestReturn(order)">
              申请退换
            </button>
          </footer>
        </li>
      </ul>
    </div>
  </section>
</template>

<script setup>
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
  }
});

const emit = defineEmits(['request-return']);

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
