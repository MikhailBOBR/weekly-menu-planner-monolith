const DAYS = ['Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота', 'Воскресенье'];

const planRoot = document.getElementById('plan-root');
const profileForm = document.getElementById('profile-form');
const generateBtn = document.getElementById('generate-btn');
const statsPanel = document.getElementById('stats-panel');
const statsTarget = document.getElementById('stats-target');
const statsAverage = document.getElementById('stats-average');
const statsWeek = document.getElementById('stats-week');
const recipesListEl = document.getElementById('recipes-list');
const recipeForm = document.getElementById('recipe-form');
const recipeFormTitle = document.getElementById('recipe-form-title');
const cancelEditBtn = document.getElementById('cancel-edit');
const deleteRecipeBtn = document.getElementById('delete-recipe');
const refreshRecipesBtn = document.getElementById('refresh-recipes');
const modal = document.getElementById('recipe-modal');
const modalBody = document.getElementById('modal-body');
const modalClose = document.getElementById('modal-close');
const clearDayPrefsBtn = document.getElementById('clear-day-prefs');
const themeToggle = document.getElementById('theme-toggle');
const helpButton = document.getElementById('help-button');
const helpModal = document.getElementById('help-modal');
const helpClose = document.getElementById('help-close');
const aboutButton = document.getElementById('about-button');
const aboutModal = document.getElementById('about-modal');
const aboutClose = document.getElementById('about-close');
const versionButton = document.getElementById('version-button');
const versionModal = document.getElementById('version-modal');
const versionClose = document.getElementById('version-close');
const notesButton = document.getElementById('notes-button');
const notesModal = document.getElementById('notes-modal');
const notesClose = document.getElementById('notes-close');
const notesField = document.getElementById('notes-field');
const notesItems = document.getElementById('notes-items');
const notesEmpty = document.getElementById('notes-empty');
const notesSave = document.getElementById('notes-save');
const notesClear = document.getElementById('notes-clear');
const weekInput = document.getElementById('weekId');
const THEME_STORAGE_KEY = 'planner-theme';
const NOTES_STORAGE_KEY = 'planner-notes';
const MAX_NOTES = 20;

const mealLabels = {
  BREAKFAST: '🍳 Завтрак',
  SNACK: '🥗 Перекус',
  LUNCH: '🍲 Обед',
  DINNER: '🍽 Ужин',
};

const state = {
  plan: null,
  recipes: [],
  editingRecipeId: null,
};

function applyTheme(theme) {
  const next = theme === 'light' ? 'light' : 'dark';
  document.body.dataset.theme = next;
  if (themeToggle) {
    themeToggle.setAttribute(
      'aria-label',
      next === 'dark' ? 'Включить светлую тему' : 'Включить тёмную тему'
    );
    themeToggle.classList.toggle('is-light', next === 'light');
  }
}

applyTheme(localStorage.getItem(THEME_STORAGE_KEY) || 'dark');

themeToggle?.addEventListener('click', () => {
  const next = document.body.dataset.theme === 'dark' ? 'light' : 'dark';
  localStorage.setItem(THEME_STORAGE_KEY, next);
  applyTheme(next);
});

profileForm.addEventListener('submit', handleGeneratePlan);
profileForm.addEventListener('reset', () => {
  setTimeout(() => {
    state.plan = null;
    planRoot.innerHTML = '';
    statsPanel.hidden = true;
    clearDayPreferences();
    ensureWeekInputValue();
  }, 0);
});

planRoot.addEventListener('click', (event) => {
  const row = event.target.closest('.meal-row');
  if (!row) return;
  const day = row.dataset.day;
  const index = Number(row.dataset.index);
  const recipe = state?.plan?.plan?.[day]?.[index];
  if (recipe) {
    openRecipeModal(recipe);
  }
});

recipesListEl.addEventListener('click', (event) => {
  const editBtn = event.target.closest('[data-edit]');
  if (!editBtn) return;
  const recipeId = Number(editBtn.dataset.edit);
  const recipe = state.recipes.find((r) => r.id === recipeId);
  if (recipe) {
    populateRecipeForm(recipe);
  }
});

recipeForm.addEventListener('submit', handleRecipeSubmit);
cancelEditBtn.addEventListener('click', resetRecipeForm);
deleteRecipeBtn.addEventListener('click', handleDeleteRecipe);
refreshRecipesBtn.addEventListener('click', loadRecipes);

modalClose.addEventListener('click', closeModal);
modal.addEventListener('click', (event) => {
  if (event.target === modal) {
    closeModal();
  }
});
helpButton?.addEventListener('click', () => {
  helpModal?.classList.remove('hidden');
});
helpClose?.addEventListener('click', () => helpModal?.classList.add('hidden'));
helpModal?.addEventListener('click', (event) => {
  if (event.target === helpModal) {
    helpModal.classList.add('hidden');
  }
});

aboutButton?.addEventListener('click', () => aboutModal?.classList.remove('hidden'));
aboutClose?.addEventListener('click', () => aboutModal?.classList.add('hidden'));
aboutModal?.addEventListener('click', (event) => {
  if (event.target === aboutModal) {
    aboutModal.classList.add('hidden');
  }
});

versionButton?.addEventListener('click', () => versionModal?.classList.remove('hidden'));
versionClose?.addEventListener('click', () => versionModal?.classList.add('hidden'));
versionModal?.addEventListener('click', (event) => {
  if (event.target === versionModal) {
    versionModal.classList.add('hidden');
  }
});

notesButton?.addEventListener('click', () => {
  loadNotes();
  notesModal?.classList.remove('hidden');
});
notesClose?.addEventListener('click', () => notesModal?.classList.add('hidden'));
notesModal?.addEventListener('click', (event) => {
  if (event.target === notesModal) {
    notesModal.classList.add('hidden');
  }
});
notesSave?.addEventListener('click', () => {
  const text = (notesField?.value || '').trim();
  if (!text) {
    alert('Напишите заметку перед сохранением.');
    return;
  }
  const entry = {
    id: Date.now(),
    text,
    createdAt: new Date().toLocaleString('ru-RU'),
  };
  const updated = [entry, ...getStoredNotes()].slice(0, MAX_NOTES);
  saveNotes(updated);
  if (notesField) notesField.value = '';
  renderNotesList(updated);
  notesModal?.classList.add('hidden');
});
notesClear?.addEventListener('click', () => {
  if (confirm('Очистить заметки?')) {
    saveNotes([]);
    if (notesField) notesField.value = '';
    renderNotesList([]);
  }
});

window.addEventListener('keydown', (event) => {
  if (event.key === 'Escape') {
    closeModal();
    helpModal?.classList.add('hidden');
    aboutModal?.classList.add('hidden');
    versionModal?.classList.add('hidden');
    notesModal?.classList.add('hidden');
  }
});

clearDayPrefsBtn?.addEventListener('click', clearDayPreferences);
ensureWeekInputValue();
loadNotes();

function ensureWeekInputValue() {
  if (!weekInput) return;
  if (!weekInput.value) {
    weekInput.value = getCurrentWeekId();
  }
}

function getCurrentWeekId(date = new Date()) {
  return formatWeekId(date);
}

function formatWeekId(date) {
  const target = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
  const dayNum = target.getUTCDay() || 7;
  target.setUTCDate(target.getUTCDate() + 4 - dayNum);
  const yearStart = new Date(Date.UTC(target.getUTCFullYear(), 0, 1));
  const weekNo = Math.ceil(((target - yearStart) / 86400000 + 1) / 7);
  return `${target.getUTCFullYear()}-W${String(weekNo).padStart(2, '0')}`;
}

function loadNotes() {
  if (!notesField) return;
  notesField.value = '';
  renderNotesList(getStoredNotes());
}

function renderNotesList(notes) {
  if (!notesItems || !notesEmpty) return;
  if (!notes || !notes.length) {
    notesItems.hidden = true;
    notesEmpty.hidden = false;
    return;
  }
  notesItems.innerHTML = notes
    .map(
      (note) => `
        <li>
          <div>${escapeHtml(note.text)}</div>
          <small style="color:var(--muted);">${note.createdAt || ''}</small>
        </li>`
    )
    .join('');
  notesItems.hidden = false;
  notesEmpty.hidden = true;
}

function escapeHtml(str) {
  return str.replace(/[&<>"']/g, (char) => {
    switch (char) {
      case '&':
        return '&amp;';
      case '<':
        return '&lt;';
      case '>':
        return '&gt;';
      case '"':
        return '&quot;';
      case "'":
        return '&#39;';
      default:
        return char;
    }
  });
}

function getStoredNotes() {
  try {
    return JSON.parse(localStorage.getItem(NOTES_STORAGE_KEY) || '[]');
  } catch (error) {
    console.warn('Не удалось прочитать заметки', error);
    return [];
  }
}

function saveNotes(notes) {
  localStorage.setItem(NOTES_STORAGE_KEY, JSON.stringify(notes));
}

async function handleGeneratePlan(event) {
  event?.preventDefault();
  setGenerateLoading(true);
  try {
    const payload = extractProfilePayload();
    const resp = await fetch('/api/generate-plan', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    if (!resp.ok) {
      throw new Error(`Ошибка ответа: ${resp.status}`);
    }
    const data = await resp.json();
    state.plan = data;
    renderPlan(data.plan);
    renderStats(data);
  } catch (error) {
    console.error(error);
    alert('Не получается собрать меню. Убедитесь, что backend запущен.');
  } finally {
    setGenerateLoading(false);
  }
}

function extractProfilePayload() {
  const formData = new FormData(profileForm);
  const toNumber = (value) => {
    const n = Number(value);
    return Number.isFinite(n) ? n : undefined;
  };
  const payload = {
    gender: formData.get('gender') || 'FEMALE',
    age: toNumber(formData.get('age')) || 30,
    height: toNumber(formData.get('height')) || 165,
    weight: toNumber(formData.get('weight')) || 60,
    activity: formData.get('activity') || 'MODERATE',
    goal: formData.get('goal') || 'MAINTAIN',
    diet: formData.get('diet') || 'ALL',
    mealsPerDay: toNumber(formData.get('mealsPerDay')) || 3,
  includeSnack: formData.get('includeSnack') === 'true',
  weekId: formData.get('weekId') || getCurrentWeekId(),
  };
  const manualCalories = toNumber(formData.get('manualCalories'));
  if (manualCalories) {
    payload.manualCalories = manualCalories;
  }
  const excluded = formData.get('excludedIngredients') || '';
  payload.excludedIngredients = excluded
    .split(',')
    .map((i) => i.trim())
    .filter(Boolean);
  payload.dayPreferences = collectDayPreferences();
  return payload;
}

function getDayField(day, role) {
  return document.querySelector(`[data-role="${role}"][data-day="${day}"]`);
}

function collectDayPreferences() {
  return DAYS.map((day) => {
    const dietEl = getDayField(day, 'day-diet');
    const cuisineEl = getDayField(day, 'day-cuisine');
    const keywordsEl = getDayField(day, 'day-keywords');

    const dietValue = dietEl?.value || '';
    const cuisine = (cuisineEl?.value || '').trim();
    const keywords = (keywordsEl?.value || '')
      .split(',')
      .map((w) => w.trim())
      .filter(Boolean);

    const hasDiet = Boolean(dietValue);
    const hasCuisine = Boolean(cuisine);
    const hasKeywords = keywords.length > 0;

    if (!hasDiet && !hasCuisine && !hasKeywords) {
      return null;
    }

    return {
      day,
      preferredDiet: hasDiet ? dietValue : null,
      preferredCuisine: hasCuisine ? cuisine : null,
      keywords,
    };
  }).filter(Boolean);
}

function clearDayPreferences() {
  DAYS.forEach((day) => {
    const dietEl = getDayField(day, 'day-diet');
    const cuisineEl = getDayField(day, 'day-cuisine');
    const keywordsEl = getDayField(day, 'day-keywords');
    if (dietEl) dietEl.value = '';
    if (cuisineEl) cuisineEl.value = '';
    if (keywordsEl) keywordsEl.value = '';
  });
}

function renderPlan(plan) {
  if (!plan || Object.keys(plan).length === 0) {
    planRoot.innerHTML =
      '<p style="text-align:center;color:#94a3b8">Добавьте рецепты или измените параметры, чтобы увидеть меню.</p>';
    return;
  }
  let html = '';
  Object.entries(plan).forEach(([day, meals = []]) => {
    const dayTotal = meals.reduce((sum, recipe) => sum + (recipe?.calories || 0), 0);
    html += `
      <div class="card">
        <div class="card-header">
          <span>${day}</span>
          <span class="day-total">${dayTotal} ккал</span>
        </div>
        <div class="card-body">
          ${meals
            .map(
              (recipe, idx) => `
              <div class="meal-row" data-day="${day}" data-index="${idx}">
                <p class="meal-title">${mealLabels[recipe.mealType] || '🍽 Блюдо'} — ${recipe.title || 'Добавьте рецепт'}</p>
                <div class="meal-meta">
                  <span>${recipe.calories ?? 0} ккал</span>
      ${
        recipe.dietType
          ? `<span class="badge" data-diet="${recipe.dietType}">${recipe.dietType}</span>`
          : ''
      }
                </div>
              </div>
            `
            )
            .join('')}
        </div>
      </div>
    `;
  });
  planRoot.innerHTML = html;
}

function renderStats(data) {
  statsTarget.textContent = `${data.targetCalories} ккал`;
  statsAverage.textContent = `${data.averageDailyCalories} ккал`;
  statsWeek.textContent = `${data.weeklyCalories} ккал`;
  statsPanel.hidden = false;
}

function setGenerateLoading(isLoading) {
  generateBtn.disabled = isLoading;
  generateBtn.textContent = isLoading ? 'Собираем меню...' : 'Собрать план';
}

async function loadRecipes() {
  try {
    const resp = await fetch('/api/recipes');
    if (!resp.ok) throw new Error('Не удалось загрузить рецепты');
    state.recipes = await resp.json();
    renderRecipeList();
  } catch (error) {
    console.error(error);
    recipesListEl.innerHTML = '<p>Не получилось загрузить рецепты 😢</p>';
  }
}

function renderRecipeList() {
  if (!state.recipes.length) {
    recipesListEl.innerHTML = '<p>Добавьте свое первое блюдо, чтобы оно появилось в меню.</p>';
    return;
  }
  recipesListEl.innerHTML = state.recipes
    .map(
      (recipe) => `
    <div class="recipe-item">
      <h4>${recipe.title}</h4>
      <div class="meta">${mealLabels[recipe.mealType] || ''} · ${recipe.calories ?? 0} ккал · ${recipe.dietType || ''}</div>
      <p>${recipe.description || 'Описание появится после сохранения.'}</p>
      <div class="mini-actions">
        <button type="button" class="outline" data-edit="${recipe.id}">Редактировать</button>
      </div>
    </div>
  `
    )
    .join('');
}

async function handleRecipeSubmit(event) {
  event.preventDefault();
  const payload = extractRecipeForm();
  const isEdit = Boolean(state.editingRecipeId);
  const url = isEdit ? `/api/recipes/${state.editingRecipeId}` : '/api/recipes';
  const method = isEdit ? 'PUT' : 'POST';

  try {
    const resp = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    if (!resp.ok) throw new Error('Не удалось сохранить рецепт');
    resetRecipeForm();
    await loadRecipes();
    if (state.plan) {
      await handleGeneratePlan();
    }
  } catch (error) {
    console.error(error);
    alert('Что-то пошло не так при сохранении рецепта.');
  }
}

function extractRecipeForm() {
  const formData = new FormData(recipeForm);
  const data = Object.fromEntries(formData.entries());
  ['calories', 'protein', 'price', 'cookingTime'].forEach((key) => {
    data[key] = data[key] ? Number(data[key]) : null;
  });
  ['imageUrl', 'description', 'ingredients', 'instructions', 'cuisine'].forEach((key) => {
    if (!data[key]) data[key] = null;
  });
  return data;
}

function populateRecipeForm(recipe) {
  state.editingRecipeId = recipe.id;
  recipeFormTitle.textContent = `Редактирование: ${recipe.title}`;
  deleteRecipeBtn.disabled = false;

  Object.entries(recipe).forEach(([key, value]) => {
    const field = recipeForm.elements.namedItem(key);
    if (!field) return;
    field.value = value ?? '';
  });
  window.scrollTo({ top: recipeForm.offsetTop - 40, behavior: 'smooth' });
}

function resetRecipeForm() {
  state.editingRecipeId = null;
  recipeForm.reset();
  recipeFormTitle.textContent = 'Новое блюдо';
  deleteRecipeBtn.disabled = true;
}

async function handleDeleteRecipe() {
  if (!state.editingRecipeId) return;
  const confirmed = confirm('Удалить этот рецепт?');
  if (!confirmed) return;
  try {
    const resp = await fetch(`/api/recipes/${state.editingRecipeId}`, { method: 'DELETE' });
    if (!resp.ok) throw new Error('Не удалось удалить рецепт');
    resetRecipeForm();
    await loadRecipes();
    if (state.plan) {
      await handleGeneratePlan();
    }
  } catch (error) {
    console.error(error);
    alert('Ошибка при удалении рецепта.');
  }
}

function openRecipeModal(recipe) {
  const ingredients = (recipe.ingredients || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item) => `<li>${item}</li>`)
    .join('');

  modalBody.innerHTML = `
    <h2>${recipe.title}</h2>
    <p>${recipe.description || ''}</p>
    <p><strong>${mealLabels[recipe.mealType] || ''}</strong> · ${recipe.calories ?? 0} ккал · ${
      recipe.protein ? recipe.protein + ' г белка' : ''
    }</p>
    <div>
      <h4>Ингредиенты</h4>
      <ul>${ingredients || '<li>Добавьте ингредиенты в карточке рецепта.</li>'}</ul>
    </div>
    <div>
      <h4>Пошаговый рецепт</h4>
      <p>${(recipe.instructions || '').replace(/\n/g, '<br />')}</p>
    </div>
  `;
  modal.classList.remove('hidden');
}

function closeModal() {
  modal.classList.add('hidden');
}

async function init() {
  deleteRecipeBtn.disabled = true;
  await loadRecipes();
  await handleGeneratePlan();
}

init();
