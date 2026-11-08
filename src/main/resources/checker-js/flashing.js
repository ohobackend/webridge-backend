() => {
    const flashingElements = [];

    // CSS 애니메이션에서 깜빡임 효과 찾기
    Array.from(document.querySelectorAll('*')).forEach(el => {
        const style = window.getComputedStyle(el);

        // blink 효과
        if (style.textDecoration && style.textDecoration.includes('blink')) {
            flashingElements.push({
                issue: 'CSS blink 사용',
                html: el.outerHTML
            });
        }

        // 빠른 애니메이션 (3Hz 초과 = 333ms 미만)
        if (style.animationDuration) {
            const duration = parseFloat(style.animationDuration.replace('s', '')) * 1000;
            if (duration < 333 && duration > 0) {
                flashingElements.push({
                    issue: `빠른 애니메이션: ${duration}ms`,
                    duration: duration,
                    html: el.outerHTML
                });
            }
        }

        // opacity 변화가 빠른 애니메이션
        if (style.animationName && style.animationName !== 'none' &&
            (el.style.opacity !== '' || (style.animation && style.animation.includes('opacity')))) {
            flashingElements.push({
                issue: '깜빡임 애니메이션 감지',
                html: el.outerHTML
            });
        }
    });

    // JavaScript로 깜빡임 효과 검사
    const scripts = Array.from(document.querySelectorAll('script')).map(s => s.textContent || '');
    const hasFlashingScript = scripts.some(script =>
        script.includes('setInterval') && (
            script.includes('opacity') ||
            script.includes('visibility') ||
            script.includes('display')
        )
    );

    return {
        flashingElements: flashingElements,
        hasFlashingScript: hasFlashingScript
    };
}
