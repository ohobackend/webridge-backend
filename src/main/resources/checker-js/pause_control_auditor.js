async (durationSeconds) => {
    const scanDuration = (typeof durationSeconds === 'number' && durationSeconds > 0) ? durationSeconds : 20;

    // --- [Part 1] Navigation Guard ---
    (function preventNavigation() {
        window.addEventListener('click', e => {
            const anchor = e.target.closest('a');
            if (anchor) { e.preventDefault(); }
        }, { capture: true });
        window.addEventListener('submit', e => { e.preventDefault(); }, { capture: true });
        window.addEventListener('beforeunload', e => { e.preventDefault(); e.returnValue = 'Audit'; });
    })();

    // --- [Part 2] Auditor Class ---
    class Auditor {
        constructor() {
            this.mutationData = new Map();
            this.scanResults = [];
            this.testedButtons = new Set();
            this.cssAnimatedElements = new Set();

            this.onAnimationStart = (e) => this.cssAnimatedElements.add(e.target);
            this.onTransitionStart = (e) => this.cssAnimatedElements.add(e.target);
        }

        async run(durationSec) {
            this.injectStyles();
            await this.startBaselineScan(durationSec);

            const movingElements = this.getMovingElements();

            for (const el of movingElements) {
                await this.testElement(el);
            }
            return this.scanResults;
        }

        injectStyles() {
            if (document.getElementById('aca-styles')) return;
            const css = `
                .aca-highlight-pass { outline: 4px solid #10b981 !important; outline-offset: -4px; }
                .aca-highlight-fail { outline: 4px solid #ef4444 !important; outline-offset: -4px; }
                .aca-highlight-testing { outline: 4px dashed #3b82f6 !important; outline-offset: -4px; }
            `;
            const style = document.createElement('style');
            style.id = 'aca-styles';
            style.innerHTML = css;
            document.head.appendChild(style);
        }

        async startBaselineScan(durationSec) {
            const durationMs = durationSec * 1000;
            document.addEventListener('animationstart', this.onAnimationStart, true);
            document.addEventListener('transitionstart', this.onTransitionStart, true);

            const observer = new MutationObserver((mutations) => {
                const now = Date.now();
                mutations.forEach((m) => {
                    if (m.type === 'attributes') {
                        const attr = m.attributeName || '';
                        const ignorePrefixes = ['data-', 'aria-', 'on'];
                        const ignoreAttrs = ['id', 'href', 'target', 'role', 'tabindex', 'rel', 'title', 'alt', 'type'];

                        if (ignorePrefixes.some(pre => attr.startsWith(pre)) || ignoreAttrs.includes(attr)) {
                            return;
                        }
                    }
                    const target = m.target.nodeType === 3 ? m.target.parentElement : m.target;
                    if (!target || target.closest('script') || target.closest('style')) return;

                    if (!this.mutationData.has(target)) {
                        this.mutationData.set(target, { count: 0, lastTime: now });
                    }
                    const data = this.mutationData.get(target);
                    data.count++;
                    data.lastTime = now;
                    this.mutationData.set(target, data);
                });
            });
            if (document.body) {
                observer.observe(document.body, { attributes: true, childList: true, subtree: true, characterData: true });
            }

            await new Promise(r => setTimeout(r, durationMs));

            observer.disconnect();
            document.removeEventListener('animationstart', this.onAnimationStart, true);
            document.removeEventListener('transitionstart', this.onTransitionStart, true);
        }

        isStrictlyHidden(el) {
            const style = window.getComputedStyle(el);
            if (style.display === 'none' || style.visibility === 'hidden' || parseFloat(style.opacity) === 0) return true;
            const rect = el.getBoundingClientRect();
            if (rect.width === 0 || rect.height === 0) return true;
            return false;
        }

        getMovingElements() {
            const candidates = new Set();
            const threshold = 2;

            // 1. Mutation 기반
            this.mutationData.forEach((data, el) => {
                if (!document.body || !document.body.contains(el)) return;
                if (this.isStrictlyHidden(el)) return;

                if (el.classList && (el.classList.contains('slick-track') || el.classList.contains('slick-list'))) {
                    if (el.parentElement) candidates.add(el.parentElement);
                    return;
                }
                if (data.count >= threshold) candidates.add(el);
            });

            // 2. CSS Animation 기반
            const allNodes = document.querySelectorAll('*');
            allNodes.forEach(el => {
                const style = window.getComputedStyle(el);
                if (style.animationName && style.animationName !== 'none' && style.animationPlayState === 'running') {
                    candidates.add(el);
                }
                if (this.cssAnimatedElements.has(el)) {
                    candidates.add(el);
                }
            });

            // 3. Video/Audio 확인
            document.querySelectorAll('video, audio').forEach(media => {
                const hasAutoplayAttr = media.hasAttribute('autoplay');
                const isPlaying = !media.paused && !media.ended;
                const hasProgress = media.currentTime > 0 && !media.ended;

                if (hasAutoplayAttr || isPlaying || hasProgress) {
                    candidates.add(media);
                }
            });

            // 4. 중복 정리
            const moving = Array.from(candidates);
            return moving.filter(el => {
                if (el.tagName === 'VIDEO' || el.tagName === 'AUDIO') return true;
                return !moving.some(parent => parent !== el && parent.contains(el));
            });
        }

        async testElement(element) {
            element.scrollIntoView({ behavior: 'smooth', block: 'center' });
            element.classList.add('aca-highlight-testing');

            const candidateInfo = this.findBestControl(element);
            let resultStatus = 'FAIL';
            let btnName = 'None';

            const innerVideo = element.querySelector('video');
            const targetMedia = element.tagName === 'VIDEO' ? element : innerVideo;

            if (candidateInfo && candidateInfo.score > 0) {
                const button = candidateInfo.element;
                btnName = (button.innerText || '').trim().substring(0, 15) ||
                          button.getAttribute('aria-label') ||
                          (button.querySelector('img') ? button.querySelector('img').alt : 'Icon');

                if (!this.testedButtons.has(button)) {
                    try { button.click(); this.testedButtons.add(button); } catch(e){}
                }

                await new Promise(r => setTimeout(r, 1500));

                if (targetMedia && targetMedia.paused) {
                    resultStatus = 'PASS';
                } else {
                    let changeCount = 0;
                    const verifyObserver = new MutationObserver((mutations) => {
                        mutations.forEach(m => {
                            if (element.contains(m.target)) changeCount++;
                        });
                    });
                    verifyObserver.observe(element, { attributes: true, childList: true, subtree: true });
                    await new Promise(r => setTimeout(r, 2000));
                    verifyObserver.disconnect();

                    if (changeCount <= 2) resultStatus = 'PASS';
                }
            }

            element.classList.remove('aca-highlight-testing');
            element.classList.add(resultStatus === 'PASS' ? 'aca-highlight-pass' : 'aca-highlight-fail');

            this.scanResults.push({
                element: element,
                tag: this.getTagInfo(element),
                status: resultStatus,
                btn: btnName,
                outerHTML: element.outerHTML.substring(0, 200)
            });
        }

        findBestControl(target) {
            let candidates = new Set();
            let current = target;
            let depth = 0;

            while (current && current !== document.body && depth < 4) {
                if (current.parentElement) {
                    current.parentElement.querySelectorAll('button, a, input, [role="button"]').forEach(s => candidates.add(s));
                }
                current = current.parentElement;
                depth++;
            }

            let bestScore = 0;
            let bestElement = null;
            candidates.forEach(c => {
                if (c === target) return;
                const score = this.calculateScore(c);
                if (score > bestScore) { bestScore = score; bestElement = c; }
            });
            return { element: bestElement, score: bestScore };
        }

        calculateScore(candidate) {
            let score = 0;
            const tag = candidate.tagName.toLowerCase();

            if (tag === 'button' || candidate.getAttribute('role') === 'button') score += 20;

            const classStr = (candidate.className && typeof candidate.className === 'string') ? candidate.className.toLowerCase() : '';
            if (classStr.includes('stop') || classStr.includes('pause')) score += 50;
            if (classStr.includes('play') || classStr.includes('ctl')) score += 10;

            const text = ((candidate.innerText || '') + (candidate.getAttribute('aria-label') || '') + (candidate.title || '')).toLowerCase();
            const img = candidate.querySelector('img');
            const altText = img ? (img.alt || '').toLowerCase() : '';
            const totalText = text + ' ' + altText;

            if (['stop', 'pause', '정지', '멈춤'].some(k => totalText.includes(k))) score += 40;

            return score;
        }

        getTagInfo(el) {
            let info = el.tagName.toLowerCase();
            if (el.id) info += `#${el.id}`;
            else if (el.className && typeof el.className === 'string') info += `.${el.className.split(' ')[0]}`;
            return info;
        }
    }

    try {
        const auditor = new Auditor();
        const rawResults = await auditor.run(scanDuration);

        return {
            autoElements: rawResults.map(item => ({
                hasAutoplay: true,
                hasPauseButton: item.status === 'PASS',
                html: item.outerHTML,
                tag: item.tag,
                debug_btn: item.btn
            })),
            animatedElementsCount: rawResults.length
        };
    } catch (e) {
        return { error: e.toString(), autoElements: [], animatedElementsCount: 0 };
    }
}
