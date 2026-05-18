import DOMPurify from 'dompurify'

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
  'a'
]

const ALLOWED_ATTR = ['href', 'title', 'target', 'rel']

function hardenLinks(container) {
  container.querySelectorAll('a[href]').forEach((link) => {
    link.setAttribute('target', '_blank')
    link.setAttribute('rel', 'noopener noreferrer')
  })
}

export function sanitizePreviewHtml(html) {
  const sanitized = DOMPurify.sanitize(html || '', {
    ALLOWED_TAGS,
    ALLOWED_ATTR,
    FORBID_TAGS: ['script', 'iframe', 'style', 'form', 'input', 'button', 'textarea', 'select', 'option'],
    FORBID_ATTR: ['style', 'class', 'id'],
    ADD_ATTR: ['target', 'rel']
  })
  const template = document.createElement('template')
  template.innerHTML = sanitized
  hardenLinks(template.content)
  return template.innerHTML
}
