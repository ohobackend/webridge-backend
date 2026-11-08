() => {
    const headings = [];
    const sections = [];

    // 제목 태그 검사
    ['h1', 'h2', 'h3', 'h4', 'h5', 'h6'].forEach(tag => {
        document.querySelectorAll(tag).forEach((heading, index) => {
            const text = heading.textContent.trim();
            const hasContent = text.length > 0;

            // 제목 다음에 본문 내용이 있는지 확인
            let hasBodyContent = false;
            let nextElement = heading.nextElementSibling;
            while (nextElement) {
                if (['H1', 'H2', 'H3', 'H4', 'H5', 'H6'].includes(nextElement.tagName)) {
                    break; // 다음 제목까지만 확인
                }
                if (nextElement.textContent.trim()) {
                    hasBodyContent = true;
                    break;
                }
                nextElement = nextElement.nextElementSibling;
            }

            headings.push({
                tag: tag,
                text: text,
                empty: !hasContent,
                hasBodyContent: hasBodyContent,
                level: parseInt(tag[1]),
                index: index,
                html: heading.outerHTML
            });
        });
    });

    // section, article, aside, nav 등 의미적 구조 검사
    document.querySelectorAll('section, article, aside, nav, main').forEach(section => {
        const hasHeading = section.querySelector('h1, h2, h3, h4, h5, h6');
        const hasContent = section.textContent.trim().length > 0;

        sections.push({
            tag: section.tagName.toLowerCase(),
            hasHeading: !!hasHeading,
            hasContent: hasContent,
            headingText: hasHeading ? hasHeading.textContent.trim() : '',
            html: section.outerHTML
        });
    });

    return { headings, sections };
}
