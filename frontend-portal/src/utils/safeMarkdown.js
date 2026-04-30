import DOMPurify from 'dompurify'
import { marked } from 'marked'

const ALLOWED_TAGS = [
  'h1',
  'h2',
  'h3',
  'h4',
  'p',
  'br',
  'strong',
  'em',
  'code',
  'pre',
  'ul',
  'ol',
  'li',
  'blockquote',
  'a',
  'table',
  'thead',
  'tbody',
  'tr',
  'th',
  'td'
]

const ALLOWED_ATTR = ['href', 'title', 'target', 'rel']

function hardenLinks(container) {
  container.querySelectorAll('a[href]').forEach((link) => {
    link.setAttribute('target', '_blank')
    link.setAttribute('rel', 'noopener noreferrer')
  })
}

export function sanitizeHtml(html) {
  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS,
    ALLOWED_ATTR,
    FORBID_TAGS: ['script', 'iframe', 'style', 'form', 'input', 'button', 'textarea', 'select', 'option'],
    FORBID_ATTR: ['style', 'class', 'id'],
    ADD_ATTR: ['target', 'rel'],
    RETURN_DOM_FRAGMENT: false
  })
}

export function renderSafeMarkdown(markdown) {
  const html = marked.parse(markdown || '', {
    async: false,
    gfm: true,
    breaks: false
  })
  const sanitized = sanitizeHtml(html)
  const template = document.createElement('template')
  template.innerHTML = sanitized
  hardenLinks(template.content)
  return template.innerHTML
}
