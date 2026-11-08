() => {
    const errors = [];

    // 빈 제목 태그들
    ['h1', 'h2', 'h3', 'h4', 'h5', 'h6'].forEach(tag => {
        document.querySelectorAll(tag).forEach(el => {
            if (!el.textContent.trim()) {
                errors.push({
                    type: `빈 ${tag.toUpperCase()} 태그 발견`,
                    tag: tag.toUpperCase(),
                    html: el.outerHTML
                });
            }
        });
    });

    // 빈 링크
    document.querySelectorAll('a').forEach(link => {
        if (!link.textContent.trim() && !link.querySelector('img')) {
            errors.push({
                type: '빈 링크 태그 발견',
                tag: 'A',
                html: link.outerHTML
            });
        }
    });

    return errors;
}
