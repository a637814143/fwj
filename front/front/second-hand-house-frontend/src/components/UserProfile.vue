<template>
  <section class="profile-card">
    <h2>个人中心</h2>
    <p v-if="!currentUser" class="hint">请登录后查看个人信息。</p>

    <div v-else class="details">
      <div class="info-grid">
        <div class="info-item">
          <span class="label">用户名</span>
          <strong>{{ currentUser.username }}</strong>
        </div>
        <div class="info-item">
          <span class="label">昵称</span>
          <strong>{{ currentUser.displayName || '—' }}</strong>
        </div>
        <div class="info-item">
          <span class="label">账号角色</span>
          <strong>{{ roleLabel || currentUser.role }}</strong>
        </div>
        <div class="info-item">
          <span class="label">实名认证</span>
          <strong :class="['status', currentUser.realNameVerified ? 'verified' : 'pending']">
            {{ currentUser.realNameVerified ? '已认证' : '未认证' }}
          </strong>
        </div>
        <div class="info-item">
          <span class="label">真实姓名</span>
          <strong>{{ currentUser.realName || '—' }}</strong>
        </div>
        <div class="info-item">
          <span class="label">手机号</span>
          <strong>{{ phoneNumber }}</strong>
        </div>
        <div class="info-item" v-if="wallet">
          <span class="label">钱包余额</span>
          <strong>{{ formatAmount(wallet.balance) }}</strong>
        </div>
        <div class="info-item" v-if="wallet && wallet.virtualPort">
          <span class="label">钱包端口号</span>
          <strong>{{ wallet.virtualPort }}</strong>
        </div>
        <div class="info-item" v-if="currentUser.createdAt">
          <span class="label">注册时间</span>
          <strong>{{ formatDateTime(currentUser.createdAt) }}</strong>
        </div>
        <div class="info-item" v-if="currentUser.lastLoginAt">
          <span class="label">最近登录</span>
          <strong>{{ formatDateTime(currentUser.lastLoginAt) }}</strong>
        </div>
      </div>
      <p class="tip">如需更新手机号或实名认证信息，请在下方完成实名认证流程。</p>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  currentUser: {
    type: Object,
    default: null
  },
  roleLabel: {
    type: String,
    default: ''
  },
  wallet: {
    type: Object,
    default: null
  }
});

const phoneNumber = computed(() => {
  if (!props.currentUser) {
    return '—';
  }
  return props.currentUser.maskedPhoneNumber || props.currentUser.phoneNumber || '—';
});

const formatAmount = (value) => {
  if (value == null) {
    return '0.00';
  }
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
};

const formatDateTime = (value) => {
  if (!value) {
    return '';
  }
  return new Date(value).toLocaleString('zh-CN', { hour12: false });
};
</script>

<style scoped>
.profile-card {
  background: #fff;
  border-radius: 1rem;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.profile-card h2 {
  margin: 0;
  font-size: 1.5rem;
  color: #1e293b;
}

.hint {
  margin: 0;
  color: #64748b;
}

.details {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.info-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.info-item {
  background: #f8fafc;
  border-radius: 0.75rem;
  padding: 0.75rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.label {
  color: #64748b;
  font-size: 0.85rem;
}

.info-item strong {
  font-size: 1rem;
  color: #1f2937;
  font-weight: 600;
  word-break: break-all;
}

.status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  border-radius: 999px;
  padding: 0.1rem 0.75rem;
}

.status.verified {
  background: rgba(34, 197, 94, 0.15);
  color: #15803d;
}

.status.pending {
  background: rgba(248, 113, 113, 0.15);
  color: #b91c1c;
}

.tip {
  margin: 0;
  font-size: 0.9rem;
  color: #475569;
}
</style>
