import './style.css';

const searchForm = document.querySelector('#search-form');
const searchInput = document.querySelector('#search-input');
const searchButton = document.querySelector('#search-button');
const results = document.querySelector('#results');
const searchFeedback = document.querySelector('#search-feedback');
const resultCount = document.querySelector('#result-count');
const dialog = document.querySelector('#show-dialog');
const detail = document.querySelector('#show-detail');
const detailFeedback = document.querySelector('#detail-feedback');
const commentForm = document.querySelector('#comment-form');
const commentFeedback = document.querySelector('#comment-feedback');
const commentSubmit = document.querySelector('#comment-submit');
let searchController;
let detailController;
let searchResults = [];
let currentShow;

async function request(path, options = {}) {
  const timeout = AbortSignal.timeout(12000);
  const signal = options.signal ? AbortSignal.any([options.signal, timeout]) : timeout;
  let response;

  try {
    response = await fetch(`/api${path}`, { ...options, signal });
  } catch (error) {
    if (options.signal?.aborted) throw error;
    throw new Error(timeout.aborted
      ? 'La consulta está tardando demasiado. Intenta de nuevo.'
      : 'No pudimos conectar. Intenta de nuevo en un momento.');
  }

  if (!response.ok) {
    const messages = {
      400: 'Revisa los datos e intenta de nuevo.',
      404: 'No encontramos esa serie.',
      502: 'No pudimos consultar las series. Intenta de nuevo en un momento.',
      503: 'El servicio no está disponible en este momento.',
    };
    throw new Error(messages[response.status] || 'Algo salió mal. Intenta de nuevo.');
  }

  return response.json();
}

function element(tag, className, text) {
  const node = document.createElement(tag);
  node.className = className;
  node.textContent = text;
  return node;
}

function plainSummary(summary) {
  if (!summary) return 'Todavía no hay una sinopsis disponible.';
  return new DOMParser().parseFromString(summary, 'text/html').body.textContent.trim();
}

function safeUrl(value) {
  try {
    const url = new URL(value);
    return ['https:', 'http:'].includes(url.protocol) ? url.href : null;
  } catch {
    return null;
  }
}

function fillGenres(container, genres = []) {
  container.replaceChildren(...(genres || []).map((genre) => element('span', 'tag', genre)));
}

function commentLabel(count) {
  return `${count} ${count === 1 ? 'opinión' : 'opiniones'}`;
}

function renderResults() {
  const template = document.querySelector('#show-card-template');
  results.replaceChildren(...searchResults.map((show, index) => {
    const card = template.content.firstElementChild.cloneNode(true);
    card.dataset.showId = show.id;
    card.querySelector('h3').textContent = show.name;
    card.querySelector('.card-channel').textContent = show.channel || 'Por descubrir';
    card.querySelector('.card-number').textContent = String(index + 1).padStart(2, '0');
    card.querySelector('.card-summary').textContent = plainSummary(show.summary);
    card.querySelector('.card-comments').textContent = commentLabel(show.comments?.length || 0);
    fillGenres(card.querySelector('.tags'), show.genres);
    const button = card.querySelector('button');
    button.setAttribute('aria-label', `Ver serie ${show.name}`);
    button.addEventListener('click', () => openShow(show.id, show.name));
    return card;
  }));
}

function showSearchMessage(title, message, error = false) {
  searchFeedback.hidden = false;
  searchFeedback.classList.toggle('error-state', error);
  searchFeedback.replaceChildren(element('span', 'empty-symbol', error ? '!' : '↗'),
    element('h3', '', title), element('p', '', message));
}

async function search(query) {
  searchController?.abort();
  const controller = new AbortController();
  searchController = controller;
  searchResults = [];
  results.replaceChildren();
  results.setAttribute('aria-busy', 'true');
  resultCount.textContent = '';
  searchButton.textContent = 'Buscando…';
  showSearchMessage('Buscando tu próxima historia', 'Un momento, estamos consultando las series.');

  try {
    const shows = await request(`/search?search_query=${encodeURIComponent(query)}`, { signal: controller.signal });
    if (controller.signal.aborted) return;
    searchResults = shows;
    document.querySelector('#results-title').textContent = `Resultados para “${query}”`;
    resultCount.textContent = `${searchResults.length} ${searchResults.length === 1 ? 'serie' : 'series'}`;
    searchFeedback.hidden = searchResults.length > 0;
    if (!searchResults.length) showSearchMessage('Esta historia no apareció', 'Prueba con otro nombre o revisa cómo se escribe.');
    renderResults();
  } catch (error) {
    if (!controller.signal.aborted) showSearchMessage('No pudimos completar la búsqueda', error.message, true);
  } finally {
    if (searchController === controller) {
      results.setAttribute('aria-busy', 'false');
      searchButton.textContent = 'Buscar ↗';
    }
  }
}

searchForm.addEventListener('submit', (event) => {
  event.preventDefault();
  const query = searchInput.value.trim();
  searchInput.setCustomValidity(query ? '' : 'Escribe el nombre de una serie.');
  if (searchForm.reportValidity()) search(query);
});

searchInput.addEventListener('input', () => searchInput.setCustomValidity(''));

document.querySelectorAll('[data-query]').forEach((button) => {
  button.addEventListener('click', () => {
    searchInput.value = button.dataset.query;
    searchInput.setCustomValidity('');
    searchForm.requestSubmit();
  });
});

async function openShow(id, name) {
  detailController?.abort();
  const controller = new AbortController();
  detailController = controller;
  currentShow = null;
  detail.hidden = true;
  detailFeedback.textContent = 'Cargando la historia…';
  document.querySelector('#dialog-title').textContent = name;
  commentForm.reset();
  document.querySelector('#comment-text').setCustomValidity('');
  commentFeedback.textContent = '';
  commentFeedback.className = '';
  commentSubmit.disabled = false;
  commentSubmit.textContent = 'Publicar comentario ↗';
  dialog.showModal();
  document.body.classList.add('dialog-open');

  try {
    const show = await request(`/show?show_id=${id}`, { signal: controller.signal });
    if (controller.signal.aborted) return;
    currentShow = show;
    document.querySelector('#dialog-title').textContent = show.name;
    document.querySelector('#show-summary').textContent = plainSummary(show.summary);
    document.querySelector('#show-meta').textContent = [show.network?.name || show.webChannel?.name,
      show.premiered?.slice(0, 4), show.runtime ? `${show.runtime} min` : null].filter(Boolean).join(' · ');
    fillGenres(document.querySelector('#show-genres'), show.genres);
    const poster = document.querySelector('#show-poster');
    const posterUrl = safeUrl(show.image?.medium);
    poster.hidden = !posterUrl;
    poster.removeAttribute('src');
    poster.alt = `Póster de ${show.name}`;
    poster.onerror = () => { poster.hidden = true; };
    if (posterUrl) poster.src = posterUrl;
    const source = document.querySelector('#show-source');
    const sourceUrl = safeUrl(show.url);
    source.hidden = !sourceUrl;
    source.removeAttribute('href');
    if (sourceUrl) source.href = sourceUrl;
    renderComments();
    detailFeedback.textContent = '';
    detail.hidden = false;
  } catch (error) {
    if (!controller.signal.aborted) detailFeedback.textContent = error.message;
  }
}

function renderComments() {
  const comments = currentShow.comments || [];
  const container = document.querySelector('#comments-list');
  document.querySelector('#comment-count').textContent = commentLabel(comments.length);
  if (!comments.length) {
    container.replaceChildren(element('p', 'no-comments', 'Aún no hay opiniones. La primera puede ser la tuya.'));
    return;
  }
  container.replaceChildren(...comments.map((comment) => {
    const item = element('article', 'comment', '');
    item.append(element('span', 'comment-rating', `★ ${comment.rating} / 5`), element('p', '', comment.comment));
    return item;
  }));
}

commentForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const show = currentShow;
  if (!show || commentSubmit.disabled) return;
  const input = document.querySelector('#comment-text');
  const comment = input.value.trim();
  input.setCustomValidity(comment ? '' : 'Escribe un comentario antes de publicarlo.');
  if (!commentForm.reportValidity()) return;
  const rating = Number(document.querySelector('#comment-rating').value);
  commentSubmit.disabled = true;
  commentSubmit.textContent = 'Publicando…';
  commentFeedback.textContent = '';
  commentFeedback.className = '';

  try {
    await request('/comments', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ show_id: show.id, comment, rating }),
    });
    show.comments = [...(show.comments || []), { comment, rating }];
    const result = searchResults.find((item) => item.id === show.id);
    if (result) {
      result.comments = show.comments;
      const count = results.querySelector(`[data-show-id="${show.id}"] .card-comments`);
      if (count) count.textContent = commentLabel(show.comments.length);
    }
    if (currentShow === show) {
      renderComments();
      commentForm.reset();
      commentFeedback.textContent = 'Tu comentario ya está publicado.';
      commentFeedback.className = 'success-message';
    }
  } catch (error) {
    if (currentShow === show) {
      commentFeedback.textContent = error.message;
      commentFeedback.className = 'error-message';
    }
  } finally {
    if (currentShow === show) {
      commentSubmit.disabled = false;
      commentSubmit.textContent = 'Publicar comentario ↗';
    }
  }
});

document.querySelector('#comment-text').addEventListener('input', (event) => event.target.setCustomValidity(''));
document.querySelector('#close-dialog').addEventListener('click', () => dialog.close());
dialog.addEventListener('click', (event) => {
  const bounds = dialog.getBoundingClientRect();
  if (event.target === dialog && (event.clientX < bounds.left || event.clientX > bounds.right
    || event.clientY < bounds.top || event.clientY > bounds.bottom)) dialog.close();
});
dialog.addEventListener('close', () => {
  detailController?.abort();
  currentShow = null;
  document.body.classList.remove('dialog-open');
});
