from pathlib import Path
r=Path('/Users/pmanko/code/openelis-work/.worktrees/amr-v2-doc-cleanup')
p=r/'mockup-viewer/src/App.jsx';s=p.read_text();pos=s.index('/** Fetch and render a markdown spec from the repo */')
helper='''/** Resolve source-relative requirement links against their document on GitHub. */
export function renderSpecMarkdown(content, specPath) {
  const renderer = new marked.Renderer();
  const renderLink = renderer.link;
  renderer.link = function (token) {
    const href = token.href;
    const sourceLink = href && (href.startsWith('#') || (!/^(?:[a-z]+:|\\/)/i.test(href) && /\\.md(?:#|$)/i.test(href)));
    return renderLink.call(this, sourceLink ? { ...token, href: new URL(href, GITHUB_BASE + specPath).href } : token);
  };
  return marked(content, { renderer });
}

'''
s=s[:pos]+helper+s[pos:]
s=s.replace('dangerouslySetInnerHTML={{ __html: marked(content) }}','dangerouslySetInnerHTML={{ __html: renderSpecMarkdown(content, specPath) }}',1)
s=s.replace('dangerouslySetInnerHTML={{ __html: marked(content) }}','dangerouslySetInnerHTML={{ __html: renderSpecMarkdown(content, mockup.specPath) }}',1)
p.write_text(s)
p=r/'mockup-viewer/src/App.test.jsx';s=p.read_text().replace('  toSlug,','  toSlug,\n  renderSpecMarkdown,',1)
s+='''

describe('rendered requirement links', () => {
  it('keeps cross-document and same-document anchors reachable from a gallery spec', () => {
    const html = renderSpecMarkdown(
      '[Media](../microbiology/amr-micro-v2-amendments.md#fr-05.1b) [This section](#acceptance) [Jira](https://uwdigi.atlassian.net/browse/OGC-1383)',
      'designs/inventory/inventory-redesign.md'
    );
    const element = document.createElement('div');
    element.innerHTML = html;
    expect(element.querySelector('a:nth-of-type(1)').href).toBe(GITHUB_BASE + 'designs/microbiology/amr-micro-v2-amendments.md#fr-05.1b');
    expect(element.querySelector('a:nth-of-type(2)').href).toBe(GITHUB_BASE + 'designs/inventory/inventory-redesign.md#acceptance');
    expect(element.querySelector('a:nth-of-type(3)').href).toBe('https://uwdigi.atlassian.net/browse/OGC-1383');
  });
});
'''
p.write_text(s)
