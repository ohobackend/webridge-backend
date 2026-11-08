() => {
    const timeouts = [];
    const intervals = [];

    // setTimeout과 setInterval 모니터링
    const originalSetTimeout = window.setTimeout;
    const originalSetInterval = window.setInterval;

    window.setTimeout = function(callback, delay) {
        if (delay && delay < 20000) {  // 20초 미만
            timeouts.push(delay);
        }
        return originalSetTimeout.apply(this, arguments);
    };

    window.setInterval = function(callback, delay) {
        if (delay) {
            intervals.push(delay);
        }
        return originalSetInterval.apply(this, arguments);
    };

    // 페이지의 모든 스크립트 태그에서 timeout 관련 검사
    const scripts = Array.from(document.querySelectorAll('script')).map(script => script.textContent || '');
    const hasTimeLimit = scripts.some(script =>
        script.includes('setTimeout') ||
        script.includes('setInterval') ||
        (script.includes('session') && script.includes('timeout'))
    );

    return {
        hasTimeLimit: hasTimeLimit,
        timeouts: timeouts,
        intervals: intervals
    };
}
