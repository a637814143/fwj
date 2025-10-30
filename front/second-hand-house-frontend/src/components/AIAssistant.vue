<template>
  <div class="ai-assistant">
    <header class="ai-assistant__header">
      <div>
        <h2>{{ t('aiAssistant.title') }}</h2>
        <p class="ai-assistant__subtitle">{{ t('aiAssistant.description') }}</p>
      </div>
      <div class="ai-assistant__header-actions">
        <span v-if="model" class="ai-assistant__model">{{ t('aiAssistant.modelLabel', { model }) }}</span>
        <button type="button" class="ghost" @click="refreshRelated" :disabled="loading">
          {{ t('aiAssistant.refresh') }}
        </button>
      </div>
    </header>

    <section class="ai-assistant__body">
      <aside class="ai-assistant__sidebar">
        <div class="ai-assistant__sidebar-header">
          <h3>{{ t('aiAssistant.relatedTitle') }}</h3>
          <p>{{ t('aiAssistant.relatedDescription') }}</p>
        </div>
        <ul class="ai-assistant__related">
          <li v-for="item in relatedQuestions" :key="item">
            <button type="button" class="link" :disabled="loading" @click="handleRelated(item)">
              {{ item }}
            </button>
          </li>
        </ul>
      </aside>

      <section class="ai-assistant__interaction">
        <form class="ai-assistant__form" @submit.prevent="submitQuestion">
          <label class="sr-only" for="ai-question">{{ t('aiAssistant.inputLabel') }}</label>
          <textarea
            id="ai-question"
            v-model="prompt"
            rows="4"
            :placeholder="t('aiAssistant.inputPlaceholder')"
            :disabled="loading"
          ></textarea>
          <div class="ai-assistant__form-actions">
            <span v-if="!hasKey" class="ai-assistant__hint">{{ t('aiAssistant.missingKey') }}</span>
            <button type="submit" class="primary" :disabled="!canSubmit">
              {{ loading ? t('aiAssistant.asking') : t('aiAssistant.ask') }}
            </button>
          </div>
        </form>

        <p v-if="error" class="ai-assistant__error">{{ error }}</p>

        <section class="ai-assistant__conversation">
          <header>
            <h3>{{ t('aiAssistant.conversationTitle') }}</h3>
          </header>
          <p v-if="exchanges.length === 0" class="ai-assistant__empty">{{ t('aiAssistant.emptyState') }}</p>
          <ul v-else>
            <li v-for="entry in exchanges" :key="entry.id" class="ai-assistant__exchange">
              <article>
                <h4>{{ t('aiAssistant.questionLabel') }}</h4>
                <p>{{ entry.question }}</p>
              </article>
              <article>
                <h4>{{ t('aiAssistant.answerLabel') }}</h4>
                <p v-if="entry.degraded" class="ai-assistant__degraded">{{ t('aiAssistant.degradedNotice') }}</p>
                <p v-if="entry.answer">{{ entry.answer }}</p>
                <p v-else class="ai-assistant__pending">{{ t('aiAssistant.pendingAnswer') }}</p>
              </article>
            </li>
          </ul>
        </section>
      </section>
    </section>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, ref, watch } from 'vue';

const props = defineProps({
  apiBaseUrl: { type: String, default: 'http://localhost:8080/api' }
});

const translate = inject('translate', (key) => key);
const t = (key, vars) => translate(key, vars);

const prompt = ref('');
const error = ref('');
const loading = ref(false);
const exchanges = ref([]);
const relatedQuestions = ref([]);
const status = ref({ ready: false, model: '' });

const normalizedApiBaseUrl = computed(() => {
  const base = typeof props.apiBaseUrl === 'string' && props.apiBaseUrl.trim().length > 0
    ? props.apiBaseUrl.trim()
    : 'http://localhost:8080/api';
  return base.replace(/\/$/, '');
});

const hasKey = computed(() => Boolean(status.value?.ready));
const model = computed(() => status.value?.model ?? '');
const canSubmit = computed(() => hasKey.value && !loading.value && prompt.value.trim().length > 0);

const suggestionPool = [
  '购买二手房时如何核实房屋的产权信息？',
  '购入二手房需要重点检查哪些装修隐患？',
  '签订二手房买卖合同前需要注意哪些条款？',
  '如何评估二手房所在小区的未来保值潜力？',
  '申请二手房按揭贷款时银行关注哪些材料？',
  '二手房交易过程中公积金贷款有哪些限制？',
  '二手房过户时需要准备哪些税费？',
  '如何判断二手房的售价是否合理？',
  '二手房交易中常见的欺诈陷阱有哪些？',
  '二手房验房时应重点关注哪些结构问题？',
  '买二手房时如何查询房屋是否有抵押？',
  '在限购城市购买二手房需要满足哪些条件？'
];

const pickRelatedQuestions = () => {
  const pool = [...suggestionPool];
  const selections = [];
  while (pool.length > 0 && selections.length < 6) {
    const index = Math.floor(Math.random() * pool.length);
    selections.push(pool.splice(index, 1)[0]);
  }
  relatedQuestions.value = selections;
};

const refreshRelated = () => {
  pickRelatedQuestions();
};

const askAssistant = async (question) => {
  const trimmed = question.trim();
  if (!trimmed) {
    return;
  }
  const entry = {
    id: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
    question: trimmed,
    answer: '',
    degraded: false
  };
  exchanges.value = [entry, ...exchanges.value];
  loading.value = true;
  error.value = '';
  try {
    const response = await fetch(`${normalizedApiBaseUrl.value}/assistant/ask`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        question: trimmed
      })
    });
    const payload = await response.json().catch(() => null);
    if (!response.ok) {
      const message =
        payload?.message ||
        payload?.error ||
        t('aiAssistant.genericError');
      throw new Error(message);
    }
    const content = typeof payload?.answer === 'string' ? payload.answer : '';
    entry.answer = typeof content === 'string' && content.trim().length > 0
      ? content.trim()
      : t('aiAssistant.fallbackAnswer');
    entry.degraded = Boolean(payload?.degraded);
    if (typeof payload?.model === 'string') {
      status.value = {
        ready: hasKey.value,
        model: payload.model
      };
    }
    exchanges.value = [...exchanges.value];
  } catch (err) {
    entry.answer = '';
    exchanges.value = [...exchanges.value];
    const message = err instanceof Error && err.message ? err.message : t('aiAssistant.genericError');
    error.value = message;
  } finally {
    loading.value = false;
    prompt.value = '';
  }
};

const submitQuestion = async () => {
  if (!canSubmit.value) {
    if (!hasKey.value) {
      error.value = t('aiAssistant.missingKey');
    }
    return;
  }
  await askAssistant(prompt.value);
};

const handleRelated = async (question) => {
  if (loading.value) {
    return;
  }
  prompt.value = question;
  await submitQuestion();
};

const loadStatus = async () => {
  try {
    const response = await fetch(`${normalizedApiBaseUrl.value}/assistant/status`);
    if (!response.ok) {
      return;
    }
    const payload = await response.json().catch(() => null);
    status.value = {
      ready: Boolean(payload?.ready),
      model: typeof payload?.model === 'string' ? payload.model : ''
    };
  } catch (loadError) {
    console.warn('Failed to load AI assistant status', loadError);
    status.value = { ready: false, model: '' };
  }
};

onMounted(() => {
  pickRelatedQuestions();
  loadStatus();
});

watch(() => normalizedApiBaseUrl.value, () => {
  loadStatus();
});
</script>

<style scoped>
.ai-assistant {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  height: 100%;
  padding: 1.5rem;
  border-radius: var(--radius-xl, 1.5rem);
  background: linear-gradient(155deg, rgba(255, 255, 255, 0.82), rgba(243, 236, 229, 0.68)),
    radial-gradient(circle at 12% 18%, rgba(180, 140, 110, 0.12), transparent 55%),
    radial-gradient(circle at 88% 28%, rgba(130, 150, 168, 0.1), transparent 60%);
  border: 1px solid color-mix(in srgb, var(--color-border, rgba(199, 189, 178, 0.42)) 65%, transparent);
  box-shadow: var(--shadow-md, 0 22px 48px rgba(105, 95, 86, 0.18));
  overflow: hidden;
}

.ai-assistant::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 20% 15%, rgba(255, 255, 255, 0.7), transparent 65%),
    radial-gradient(circle at 80% 85%, rgba(214, 188, 166, 0.18), transparent 70%);
  mix-blend-mode: screen;
  opacity: 0.7;
  pointer-events: none;
}

.ai-assistant__header {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  padding: 1.25rem 1.5rem;
  border-radius: var(--radius-lg, 1.25rem);
  background: color-mix(in srgb, var(--panel-card-bg, rgba(255, 255, 255, 0.94)) 90%, transparent);
  box-shadow: 0 18px 32px rgba(120, 110, 100, 0.14);
  backdrop-filter: blur(18px);
}

.ai-assistant__header h2 {
  margin: 0;
  font-family: var(--font-sans, 'Noto Sans SC', sans-serif);
  font-size: 2rem;
  letter-spacing: 0.04em;
  color: color-mix(in srgb, var(--color-text-strong) 86%, #b48c6e 14%);
}

.ai-assistant__subtitle {
  color: var(--color-text-muted, #746a5e);
  margin-top: 0.35rem;
  font-size: 1rem;
  line-height: 1.55;
  font-family: var(--font-sans, 'Noto Sans SC', sans-serif);
  letter-spacing: 0.03em;
}

.ai-assistant__header-actions {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.ai-assistant__header-actions button {
  border-radius: var(--radius-pill, 999px);
  padding: 0.5rem 1.1rem;
  background: var(--gradient-soft, linear-gradient(135deg, rgba(180, 140, 110, 0.18), rgba(154, 161, 168, 0.16)));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.35);
}

.ai-assistant__header-actions button:hover:not(:disabled) {
  transform: translateY(-1px);
}

.ai-assistant__model {
  font-size: 0.9rem;
  color: color-mix(in srgb, var(--color-text-muted, #746a5e) 80%, rgba(180, 140, 110, 0.8) 20%);
  font-family: var(--font-sans, 'Noto Sans SC', sans-serif);
}

.ai-assistant__body {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(18rem, 22rem) 1fr;
  gap: 2.25rem;
  height: 100%;
}

.ai-assistant__sidebar {
  position: relative;
  background: color-mix(in srgb, var(--panel-card-soft-bg, rgba(247, 242, 236, 0.82)) 92%, transparent);
  border: 1px solid color-mix(in srgb, var(--color-border-strong, rgba(189, 177, 165, 0.55)) 55%, transparent);
  border-radius: var(--radius-lg, 1.25rem);
  padding: 1.4rem 1.6rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.55), 0 16px 34px rgba(150, 132, 118, 0.18);
  backdrop-filter: blur(16px);
}

.ai-assistant__sidebar::after {
  content: '';
  position: absolute;
  inset: 12% 8% 10% 12%;
  border-radius: inherit;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.12), rgba(180, 140, 110, 0.08));
  opacity: 0.8;
  pointer-events: none;
}

.ai-assistant__sidebar-header h3 {
  margin: 0 0 0.35rem;
  font-family: var(--font-sans, 'Noto Sans SC', sans-serif);
  font-size: 1.35rem;
  color: color-mix(in srgb, var(--color-text-strong) 80%, #b48c6e 20%);
}

.ai-assistant__sidebar-header p {
  font-size: 0.95rem;
  color: color-mix(in srgb, var(--color-text-muted, #746a5e) 85%, rgba(255, 255, 255, 0.6) 15%);
  line-height: 1.5;
}

.ai-assistant__related {
  display: grid;
  gap: 0.75rem;
  list-style: none;
  padding: 0;
  margin: 0;
}

.ai-assistant__related button {
  text-align: left;
  color: color-mix(in srgb, var(--color-accent, #b48c6e) 70%, var(--color-text-strong) 30%);
  background: linear-gradient(120deg, rgba(255, 255, 255, 0.82), rgba(255, 255, 255, 0));
  border: none;
  padding: 0.55rem 0.75rem;
  border-radius: var(--radius-md, 0.85rem);
  font-size: 1rem;
  font-family: var(--font-sans, 'Noto Sans SC', sans-serif);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.ai-assistant__related button:hover:not(:disabled) {
  transform: translateX(6px);
  box-shadow: 0 8px 16px rgba(180, 140, 110, 0.16);
}

.ai-assistant__related button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ai-assistant__interaction {
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
}

.ai-assistant__form {
  position: relative;
  padding: 1.5rem;
  border-radius: var(--radius-lg, 1.25rem);
  background: color-mix(in srgb, var(--panel-card-bg, rgba(255, 255, 255, 0.94)) 96%, transparent);
  border: 1px solid color-mix(in srgb, var(--color-border, rgba(199, 189, 178, 0.42)) 65%, transparent);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.55), 0 18px 32px rgba(150, 132, 118, 0.16);
  backdrop-filter: blur(14px);
}

.ai-assistant__form::after {
  content: '';
  position: absolute;
  inset: 12px;
  border-radius: inherit;
  border: 1px dashed rgba(180, 140, 110, 0.25);
  pointer-events: none;
}

.ai-assistant__form textarea {
  width: 100%;
  border: none;
  border-radius: var(--radius-md, 0.85rem);
  padding: 0.95rem 1.1rem;
  font-size: 1.05rem;
  line-height: 1.6;
  resize: vertical;
  min-height: 7rem;
  background: linear-gradient(130deg, rgba(255, 255, 255, 0.92), rgba(243, 236, 229, 0.86));
  box-shadow: inset 0 0 0 1px rgba(180, 140, 110, 0.12);
  font-family: var(--font-sans, 'Noto Sans SC', sans-serif);
  color: var(--color-text-strong);
}

.ai-assistant__form textarea:focus {
  outline: none;
  box-shadow: inset 0 0 0 2px rgba(180, 140, 110, 0.32), 0 18px 30px rgba(180, 140, 110, 0.18);
}

.ai-assistant__form textarea:disabled {
  background: rgba(249, 250, 251, 0.75);
}

.ai-assistant__form-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 0.85rem;
  gap: 1rem;
}

.ai-assistant__form-actions .primary {
  border-radius: var(--radius-pill, 999px);
  padding: 0.65rem 1.6rem;
  font-size: 1rem;
  font-family: var(--font-sans, 'Noto Sans SC', sans-serif);
  background: linear-gradient(135deg, rgba(180, 140, 110, 0.92), rgba(122, 142, 160, 0.9));
  color: var(--color-text-on-emphasis, #faf7f3);
  box-shadow: 0 15px 28px rgba(150, 132, 118, 0.28);
}

.ai-assistant__form-actions .primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 18px 36px rgba(150, 132, 118, 0.32);
}

.ai-assistant__hint {
  font-size: 0.95rem;
  color: #d86c6c;
  font-family: var(--font-sans, 'Noto Sans SC', sans-serif);
}

.ai-assistant__error {
  color: #d86c6c;
  background: rgba(216, 108, 108, 0.12);
  border-radius: var(--radius-md, 0.85rem);
  padding: 0.85rem 1.2rem;
  border: 1px solid rgba(216, 108, 108, 0.35);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.4);
  font-family: var(--font-sans, 'Noto Sans SC', sans-serif);
}

.ai-assistant__conversation {
  padding: 1.5rem;
  border-radius: var(--radius-lg, 1.25rem);
  background: color-mix(in srgb, var(--panel-card-tint, rgba(236, 230, 223, 0.72)) 90%, transparent);
  border: 1px solid color-mix(in srgb, var(--color-border-strong, rgba(189, 177, 165, 0.55)) 50%, transparent);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.4), 0 20px 38px rgba(140, 126, 112, 0.18);
  backdrop-filter: blur(12px);
}

.ai-assistant__conversation header h3 {
  margin: 0 0 1rem;
  font-family: var(--font-sans, 'Noto Sans SC', sans-serif);
  font-size: 1.5rem;
  color: color-mix(in srgb, var(--color-text-strong) 82%, #b48c6e 18%);
}

.ai-assistant__conversation ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.ai-assistant__exchange {
  position: relative;
  background: linear-gradient(140deg, rgba(255, 255, 255, 0.95), rgba(244, 238, 232, 0.8));
  border: 1px solid rgba(180, 140, 110, 0.18);
  border-radius: var(--radius-lg, 1.25rem);
  padding: 1.25rem 1.4rem;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  box-shadow: 0 16px 32px rgba(150, 132, 118, 0.16);
}

.ai-assistant__exchange::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: linear-gradient(120deg, rgba(180, 140, 110, 0.12), rgba(180, 140, 110, 0));
  opacity: 0.8;
  pointer-events: none;
}

.ai-assistant__exchange article {
  position: relative;
  z-index: 1;
  padding-left: 0.5rem;
  border-left: 3px solid rgba(180, 140, 110, 0.35);
}

.ai-assistant__exchange article h4 {
  font-size: 1rem;
  font-weight: 600;
  color: color-mix(in srgb, var(--color-text-muted, #746a5e) 82%, rgba(180, 140, 110, 0.8) 18%);
  margin: 0 0 0.45rem;
  font-family: var(--font-sans, 'Noto Sans SC', sans-serif);
  letter-spacing: 0.03em;
}

.ai-assistant__exchange article p {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.65;
  font-size: 1.05rem;
  font-family: var(--font-sans, 'Noto Sans SC', sans-serif);
  color: var(--color-text-strong);
}

.ai-assistant__degraded {
  color: color-mix(in srgb, var(--warning-strong, #b45309) 70%, #d57a3d 30%);
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.ai-assistant__pending {
  color: color-mix(in srgb, var(--color-text-muted, #746a5e) 70%, rgba(180, 140, 110, 0.6) 30%);
  font-style: italic;
}

.ai-assistant__empty {
  color: color-mix(in srgb, var(--color-text-muted, #746a5e) 80%, rgba(255, 255, 255, 0.5) 20%);
  font-family: var(--font-sans, 'Noto Sans SC', sans-serif);
  text-align: center;
  padding: 2rem 1rem;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82), rgba(255, 255, 255, 0));
  border-radius: var(--radius-lg, 1.25rem);
}

@media (max-width: 1024px) {
  .ai-assistant {
    padding: 1.25rem;
  }

  .ai-assistant__body {
    grid-template-columns: 1fr;
    gap: 1.75rem;
  }

  .ai-assistant__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
  }
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
</style>
