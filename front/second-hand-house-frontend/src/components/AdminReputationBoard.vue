<template>
  <section class="admin-board">
    <header>
      <div>
        <h2>{{ t('adminReputation.title') }}</h2>
        <p>{{ t('adminReputation.subtitle') }}</p>
      </div>
      <button type="button" @click="$emit('refresh')">{{ t('adminReputation.refresh') }}</button>
    </header>

    <div v-if="loading" class="loading">{{ t('adminReputation.loading') }}</div>
    <div v-else class="content">
      <section class="summary" v-if="overview">
        <div>
          <span class="label">{{ t('adminReputation.summary.blacklisted') }}</span>
          <strong>{{ overview.blacklistedCount }}</strong>
        </div>
        <div>
          <span class="label">{{ t('adminReputation.summary.sellers') }}</span>
          <strong>{{ overview.sellers.length }}</strong>
        </div>
        <div>
          <span class="label">{{ t('adminReputation.summary.buyers') }}</span>
          <strong>{{ overview.buyers.length }}</strong>
        </div>
      </section>

      <section class="leaderboard">
        <div>
          <h3>{{ t('adminReputation.tables.sellers.title') }}</h3>
          <table>
            <thead>
              <tr>
                <th>{{ t('adminReputation.tables.sellers.columns.account') }}</th>
                <th>{{ t('adminReputation.tables.sellers.columns.reputation') }}</th>
                <th>{{ t('adminReputation.tables.sellers.columns.breaches') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="seller in overview?.sellers ?? []" :key="`seller-${seller.username}`">
                <td>
                  <strong>{{ seller.displayName }}</strong>
                  <span class="username">@{{ seller.username }}</span>
                </td>
                <td>{{ formatNumber(seller.reputationScore) }}</td>
                <td>{{ seller.reservationBreaches }}</td>
              </tr>
              <tr v-if="!overview || overview.sellers.length === 0">
                <td colspan="3" class="empty">{{ t('adminReputation.tables.sellers.empty') }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div>
          <h3>{{ t('adminReputation.tables.buyers.title') }}</h3>
          <table>
            <thead>
              <tr>
                <th>{{ t('adminReputation.tables.buyers.columns.account') }}</th>
                <th>{{ t('adminReputation.tables.buyers.columns.reputation') }}</th>
                <th>{{ t('adminReputation.tables.buyers.columns.returns') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="buyer in overview?.buyers ?? []" :key="`buyer-${buyer.username}`">
                <td>
                  <strong>{{ buyer.displayName }}</strong>
                  <span class="username">@{{ buyer.username }}</span>
                </td>
                <td>{{ formatNumber(buyer.reputationScore) }}</td>
                <td>{{ buyer.returnCount }}</td>
              </tr>
              <tr v-if="!overview || overview.buyers.length === 0">
                <td colspan="3" class="empty">{{ t('adminReputation.tables.buyers.empty') }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="user-table">
        <h3>{{ t('adminReputation.blacklist.title') }}</h3>
        <table>
          <thead>
            <tr>
              <th>{{ t('adminReputation.blacklist.columns.account') }}</th>
              <th>{{ t('adminReputation.blacklist.columns.role') }}</th>
              <th>{{ t('adminReputation.blacklist.columns.reputation') }}</th>
              <th>{{ t('adminReputation.blacklist.columns.behaviour') }}</th>
              <th>{{ t('adminReputation.blacklist.columns.status') }}</th>
              <th>{{ t('adminReputation.blacklist.columns.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.username" :class="{ blacklisted: user.blacklisted }">
              <td>
                <strong>{{ user.displayName }}</strong>
                <span class="username">@{{ user.username }}</span>
              </td>
              <td>{{ roleLabels[user.role] ?? user.role }}</td>
              <td>{{ formatNumber(user.reputationScore) }}</td>
              <td>
                <span v-if="isSellerRole(user.role)">
                  {{
                    t('adminReputation.blacklist.breaches', {
                      count: Number.isFinite(Number(user.reservationBreaches))
                        ? Number(user.reservationBreaches)
                        : 0
                    })
                  }}
                </span>
                <span v-else>
                  {{
                    t('adminReputation.blacklist.returns', {
                      count: Number.isFinite(Number(user.returnCount)) ? Number(user.returnCount) : 0
                    })
                  }}
                </span>
              </td>
              <td>
                <span class="status" :class="user.blacklisted ? 'bad' : 'good'">
                  {{ user.blacklisted ? t('adminReputation.status.blacklisted') : t('adminReputation.status.active') }}
                </span>
              </td>
              <td>
                <div class="actions">
                  <button
                    type="button"
                    :disabled="user.username === currentUser?.username"
                    @click="toggle(user)"
                  >
                    {{
                      user.blacklisted
                        ? t('adminReputation.actions.removeBlacklist')
                        : t('adminReputation.actions.addBlacklist')
                    }}
                  </button>
                  <button
                    type="button"
                    class="danger"
                    :disabled="user.role === 'ADMIN' || user.username === currentUser?.username"
                    @click="remove(user)"
                  >
                    {{ t('adminReputation.actions.deleteAccount') }}
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!users || users.length === 0">
              <td colspan="6" class="empty">{{ t('adminReputation.blacklist.empty') }}</td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, inject } from 'vue';

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  overview: {
    type: Object,
    default: null
  },
  users: {
    type: Array,
    default: () => []
  },
  currentUser: {
    type: Object,
    default: null
  }
});

const emit = defineEmits(['toggle-blacklist', 'refresh', 'delete-user']);

const translate = inject('translate', (key, vars) => key);
const settings = inject('appSettings', { language: 'zh' });
const t = (key, vars) => translate(key, vars);

const locale = computed(() => (settings?.language === 'en' ? 'en-US' : 'zh-CN'));

const sellerRoles = ['SELLER', 'LANDLORD'];

const roleLabels = computed(() => ({
  SELLER: t('roles.seller'),
  LANDLORD: t('roles.seller'),
  BUYER: t('roles.buyer'),
  ADMIN: t('roles.admin')
}));

const isSellerRole = (role) => sellerRoles.includes(role);

const formatNumber = (value) => {
  if (value == null || Number.isNaN(Number(value))) {
    return '0';
  }
  return Number(value).toLocaleString(locale.value, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  });
};

const toggle = (user) => {
  emit('toggle-blacklist', { username: user.username, blacklisted: !user.blacklisted });
};

const remove = (user) => {
  emit('delete-user', { username: user.username });
};
</script>

<style scoped>
.admin-board {
  display: flex;
  flex-direction: column;
  gap: 1.6rem;
  background: var(--gradient-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  padding: 1.85rem;
  border: 1px solid var(--color-border);
  backdrop-filter: blur(var(--glass-blur));
}

header {
  display: flex;
  justify-content: space-between;
  gap: 1.1rem;
  flex-wrap: wrap;
  align-items: center;
}

header h2 {
  margin: 0;
  color: var(--color-text-strong);
}

header p {
  margin: 0.4rem 0 0;
  color: var(--color-text-muted);
  max-width: 36rem;
}

header button {
  border: none;
  border-radius: var(--radius-pill);
  background: var(--gradient-primary);
  color: #fff;
  padding: 0.6rem 1.6rem;
  cursor: pointer;
  font-weight: 600;
  box-shadow: 0 18px 34px rgba(37, 99, 235, 0.28);
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

header button:hover {
  transform: translateY(-2px);
  box-shadow: 0 26px 48px rgba(37, 99, 235, 0.32);
}

.loading {
  text-align: center;
  padding: 1.6rem;
  color: var(--color-text-muted);
}

.summary {
  display: grid;
  gap: 1.1rem;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.summary > div {
  background: rgba(37, 99, 235, 0.12);
  border-radius: var(--radius-lg);
  padding: 1.1rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  border: 1px solid rgba(37, 99, 235, 0.25);
}

.summary .label {
  font-size: 0.88rem;
  color: #1d4ed8;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.summary strong {
  font-size: 1.55rem;
  color: var(--color-text-strong);
}

.actions {
  display: flex;
  gap: 0.55rem;
  flex-wrap: wrap;
}

.actions .danger {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff;
  box-shadow: 0 16px 32px rgba(239, 68, 68, 0.25);
}

.actions .danger:disabled {
  background: rgba(252, 165, 165, 0.85);
  cursor: not-allowed;
  box-shadow: none;
}

.leaderboard {
  display: grid;
  gap: 1.6rem;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
}

.leaderboard table,
.user-table table {
  width: 100%;
  border-collapse: collapse;
  background: rgba(248, 250, 252, 0.92);
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.leaderboard th,
.leaderboard td,
.user-table th,
.user-table td {
  padding: 0.8rem 1.1rem;
  border-bottom: 1px solid rgba(226, 232, 240, 0.65);
  text-align: left;
  font-size: 0.92rem;
}

.leaderboard th,
.user-table th {
  background: rgba(224, 231, 255, 0.85);
  color: #1e3a8a;
  font-weight: 700;
}

.username {
  display: block;
  font-size: 0.78rem;
  color: var(--color-text-soft);
}

.empty {
  text-align: center;
  color: rgba(148, 163, 184, 0.9);
  font-size: 0.92rem;
}

.user-table {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.user-table h3 {
  margin: 0;
  color: var(--color-text-strong);
}

.user-table button {
  border: none;
  border-radius: var(--radius-pill);
  padding: 0.45rem 1rem;
  cursor: pointer;
  font-weight: 600;
  background: var(--gradient-primary);
  color: #fff;
  box-shadow: 0 14px 28px rgba(37, 99, 235, 0.25);
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}

.user-table button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 22px 40px rgba(37, 99, 235, 0.28);
}

.user-table button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  box-shadow: none;
}

.user-table tr.blacklisted {
  background: rgba(239, 68, 68, 0.08);
}

.status {
  padding: 0.35rem 0.85rem;
  border-radius: var(--radius-pill);
  font-weight: 600;
  font-size: 0.88rem;
}

.status.good {
  background: rgba(34, 197, 94, 0.18);
  color: #16a34a;
}

.status.bad {
  background: rgba(239, 68, 68, 0.2);
  color: #dc2626;
}
</style>
