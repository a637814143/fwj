<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="contract-agreement__backdrop"
      role="dialog"
      aria-modal="true"
      :aria-label="agreementTitle"
      @click.self="handleReject"
    >
      <div class="contract-agreement__modal">
        <header class="contract-agreement__header">
          <div>
            <h2>{{ agreementTitle }}</h2>
            <p class="contract-agreement__context">{{ contextNotice }}</p>
          </div>
          <button type="button" class="contract-agreement__close" @click="handleReject">×</button>
        </header>
        <section ref="scrollContainer" class="contract-agreement__content" @scroll="handleScroll">
          <article class="contract-agreement__article">
            <p class="contract-agreement__preface">{{ contract.preface }}</p>
            <section
              v-for="section in contract.sections"
              :key="section.heading"
              class="contract-agreement__section"
            >
              <h3>{{ section.heading }}</h3>
              <ol>
                <li v-for="(clause, index) in section.clauses" :key="`${section.heading}-${index}`">{{ clause }}</li>
              </ol>
            </section>
            <section v-if="contract.appendix" class="contract-agreement__section">
              <h3>{{ contract.appendix.heading }}</h3>
              <ul>
                <li v-for="(item, index) in contract.appendix.clauses" :key="`appendix-${index}`">{{ item }}</li>
              </ul>
            </section>
            <p v-for="(item, index) in contract.conclusion" :key="`conclusion-${index}`" class="contract-agreement__conclusion">
              {{ item }}
            </p>
          </article>
        </section>
        <footer class="contract-agreement__footer">
          <p class="contract-agreement__notice">{{ scrollNotice }}</p>
          <div class="contract-agreement__actions">
            <button type="button" class="btn ghost" :disabled="!actionsEnabled" @click="handleReject">
              {{ disagreeLabel }}
            </button>
            <button type="button" class="btn primary" :disabled="!actionsEnabled" @click="handleAgree">
              {{ agreeLabel }}
            </button>
          </div>
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { Teleport, computed, inject, nextTick, onMounted, ref, watch } from 'vue';

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  context: {
    type: String,
    default: 'buyer'
  }
});

const emit = defineEmits(['agree', 'reject']);

const translate = inject('translate', (key) => key);
const t = (path, vars) => translate(path, vars);

const scrollContainer = ref(null);
const hasReachedEnd = ref(false);

const resolveContract = () => {
  const contract = t('contracts.secondHand');
  if (contract && typeof contract === 'object') {
    return contract;
  }
  return {
    title: '',
    preface: '',
    sections: [],
    conclusion: []
  };
};

const contract = computed(resolveContract);
const agreementTitle = computed(() => contract.value.title || '');

const usage = computed(() => {
  const raw = t('contracts.usage');
  return raw && typeof raw === 'object'
    ? raw
    : { seller: { notice: '' }, buyer: { notice: '' } };
});

const contextNotice = computed(() => {
  if (props.context === 'seller') {
    return usage.value.seller?.notice ?? '';
  }
  return usage.value.buyer?.notice ?? '';
});

const scrollNotice = computed(() => t('contracts.common.scrollNotice'));
const disagreeLabel = computed(() => t('contracts.common.disagree'));
const agreeLabel = computed(() => {
  const labelMap = t('contracts.common.agreeLabels');
  if (labelMap && typeof labelMap === 'object') {
    return props.context === 'seller'
      ? labelMap.seller ?? labelMap.default
      : labelMap.buyer ?? labelMap.default;
  }
  return t('contracts.common.agree');
});

const actionsEnabled = computed(() => hasReachedEnd.value);

const updateScrollState = () => {
  const el = scrollContainer.value;
  if (!el) {
    hasReachedEnd.value = false;
    return;
  }
  if (el.scrollHeight <= el.clientHeight + 2) {
    hasReachedEnd.value = true;
    return;
  }
  const distance = el.scrollTop + el.clientHeight;
  hasReachedEnd.value = distance >= el.scrollHeight - 8;
};

const resetScroll = () => {
  hasReachedEnd.value = false;
  nextTick(() => {
    const el = scrollContainer.value;
    if (el) {
      el.scrollTop = 0;
      updateScrollState();
    }
  });
};

onMounted(() => {
  if (props.visible) {
    resetScroll();
  }
});

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      resetScroll();
    }
  }
);

const handleScroll = () => {
  updateScrollState();
};

const handleAgree = () => {
  if (!actionsEnabled.value) {
    return;
  }
  emit('agree');
};

const handleReject = () => {
  if (!actionsEnabled.value) {
    return;
  }
  emit('reject');
};
</script>

<style scoped>
.contract-agreement__backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(6px);
  display: grid;
  place-items: center;
  z-index: 2000;
  padding: 2rem 1.5rem;
}

.contract-agreement__modal {
  width: min(960px, 100%);
  max-height: min(90vh, 960px);
  background: var(--color-surface, #ffffff);
  border-radius: var(--radius-xl, 1.25rem);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.35);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--color-border, rgba(226, 232, 240, 0.7)) 85%, transparent);
}

.contract-agreement__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  padding: 1.75rem 2rem 1.25rem;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), rgba(14, 165, 233, 0.05));
}

.contract-agreement__header h2 {
  margin: 0;
  font-size: 1.4rem;
  color: var(--color-text-strong, #0f172a);
}

.contract-agreement__context {
  margin: 0.5rem 0 0;
  color: var(--color-text-muted, #475569);
  font-size: 0.95rem;
}

.contract-agreement__close {
  border: none;
  background: transparent;
  font-size: 1.5rem;
  line-height: 1;
  color: var(--color-text-muted, #475569);
  cursor: pointer;
  padding: 0.25rem;
}

.contract-agreement__close:hover {
  color: var(--color-text-strong, #0f172a);
}

.contract-agreement__content {
  flex: 1;
  overflow-y: auto;
  padding: 0 2rem 1.5rem;
}

.contract-agreement__article {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  color: var(--color-text-strong, #0f172a);
}

.contract-agreement__preface {
  margin: 1.5rem 0 0.5rem;
  line-height: 1.75;
  font-size: 1rem;
}

.contract-agreement__section h3 {
  margin: 0 0 0.75rem;
  font-size: 1.05rem;
  color: var(--color-text-strong, #0f172a);
}

.contract-agreement__section ol,
.contract-agreement__section ul {
  margin: 0;
  padding-left: 1.2rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  line-height: 1.7;
}

.contract-agreement__conclusion {
  margin: 0;
  line-height: 1.7;
  color: var(--color-text-muted, #475569);
}

.contract-agreement__footer {
  padding: 1.25rem 2rem 1.75rem;
  border-top: 1px solid color-mix(in srgb, var(--color-border, rgba(226, 232, 240, 0.7)) 85%, transparent);
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.08), rgba(14, 165, 233, 0.04));
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.contract-agreement__notice {
  margin: 0;
  font-size: 0.95rem;
  color: var(--color-text-muted, #475569);
}

.contract-agreement__actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
}

.btn {
  border-radius: var(--radius-pill, 999px);
  padding: 0.65rem 1.5rem;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn.primary {
  background: var(--color-primary, #2563eb);
  color: #ffffff;
  border: none;
}

.btn.primary:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.btn.ghost {
  background: transparent;
  color: var(--color-text-strong, #0f172a);
  border: 1px solid color-mix(in srgb, var(--color-border, rgba(148, 163, 184, 0.4)) 80%, transparent);
}

.btn.ghost:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

:global(body[data-theme='dark']) .contract-agreement__modal {
  background: rgba(15, 23, 42, 0.92);
  border-color: rgba(148, 163, 184, 0.25);
  box-shadow: 0 24px 60px rgba(2, 6, 23, 0.7);
}

:global(body[data-theme='dark']) .contract-agreement__header {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.25), rgba(13, 148, 136, 0.18));
}

:global(body[data-theme='dark']) .contract-agreement__footer {
  background: linear-gradient(135deg, rgba(30, 64, 175, 0.2), rgba(15, 118, 110, 0.12));
}

:global(body[data-theme='dark']) .contract-agreement__article,
:global(body[data-theme='dark']) .contract-agreement__section h3,
:global(body[data-theme='dark']) .contract-agreement__conclusion {
  color: rgba(226, 232, 240, 0.92);
}

:global(body[data-theme='dark']) .contract-agreement__context,
:global(body[data-theme='dark']) .contract-agreement__notice {
  color: rgba(148, 163, 184, 0.85);
}

:global(body[data-theme='dark']) .btn.ghost {
  color: rgba(226, 232, 240, 0.92);
  border-color: rgba(148, 163, 184, 0.4);
}
</style>
