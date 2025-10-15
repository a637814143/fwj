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
              <dt>金额（万元）</dt>
              <dd>{{ formatAmount(order.amount) }}</dd>
            </div>
            <div>
              <dt>买家</dt>
              <dd>{{ order.buyerDisplayName }}（{{ order.buyerUsername }}）</dd>
            </div>
            <div>
              <dt>卖家</dt>
              <dd>{{ order.sellerDisplayName }}（{{ order.sellerUsername }}）</dd>
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
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
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
  gap: 1rem;
  background: #fff;
  border-radius: 1rem;
  box-shadow: 0 10px 25px rgba(15, 23, 42, 0.08);
  padding: 1.5rem;
}

.order-history h2 {
  margin: 0;
  font-size: 1.5rem;
  color: #1e293b;
}

.hint {
  margin: 0;
  color: #64748b;
}

.loading,
.empty {
  background: #f8fafc;
  border-radius: 0.75rem;
  padding: 1rem 1.25rem;
  color: #475569;
}

.order-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.order-card {
  border: 1px solid #e2e8f0;
  border-radius: 0.9rem;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.order-card header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.order-card h3 {
  margin: 0;
  color: #1e293b;
}

.status {
  border-radius: 999px;
  padding: 0.25rem 0.75rem;
  font-size: 0.85rem;
  font-weight: 600;
}

.status.paid {
  background: rgba(34, 197, 94, 0.15);
  color: #15803d;
}

.status.pending {
  background: rgba(59, 130, 246, 0.2);
  color: #1d4ed8;
}

.status.returned {
  background: rgba(248, 113, 113, 0.2);
  color: #b91c1c;
}

.status.reserved {
  background: rgba(250, 204, 21, 0.2);
  color: #b45309;
}

.status.cancelled {
  background: rgba(148, 163, 184, 0.25);
  color: #475569;
}

dl {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 0.75rem 1.25rem;
  margin: 0;
}

dl div {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

dl dt {
  color: #475569;
  font-size: 0.9rem;
}

dl dd {
  margin: 0;
  color: #1f2937;
  font-size: 0.95rem;
}

.actions {
  display: flex;
  justify-content: flex-end;
}

.actions button {
  background: #f97316;
  border: none;
  border-radius: 0.75rem;
  color: #fff;
  cursor: pointer;
  font-weight: 600;
  padding: 0.55rem 1.4rem;
}

.actions button:hover {
  background: #ea580c;
}

.actions button:disabled {
  background: #fdba74;
  cursor: not-allowed;
}
</style>
