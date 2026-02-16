(function () {
    const API_PREFIX = "/api";
    const ENDPOINT = `${API_PREFIX}/tutoring/demands`;

    const fallbackData = [
        {
            id: "D-1001",
            title: "初二数学提分冲刺",
            city: "深圳",
            district: "南山",
            grade: "初二",
            subject: "数学",
            salaryMin: 180,
            salaryMax: 260,
            createdAt: "2026-02-16 10:30",
            description: "目标期中考试提升 15 分，周内晚间可上课，优先有竞赛背景老师。",
            location: "南山区科技园地铁站附近"
        },
        {
            id: "D-1002",
            title: "高一物理基础补齐",
            city: "深圳",
            district: "福田",
            grade: "高一",
            subject: "物理",
            salaryMin: 220,
            salaryMax: 320,
            createdAt: "2026-02-16 09:12",
            description: "孩子概念薄弱，需要从受力分析和运动学重新梳理。",
            location: "福田区石厦"
        },
        {
            id: "D-1003",
            title: "五年级英语口语训练",
            city: "广州",
            district: "天河",
            grade: "五年级",
            subject: "英语",
            salaryMin: 140,
            salaryMax: 200,
            createdAt: "2026-02-15 19:50",
            description: "希望以场景对话为主，能纠正发音并提升表达自信。",
            location: "天河区员村"
        },
        {
            id: "D-1004",
            title: "高三化学一轮复习",
            city: "北京",
            district: "海淀",
            grade: "高三",
            subject: "化学",
            salaryMin: 280,
            salaryMax: 420,
            createdAt: "2026-02-14 15:20",
            description: "重点提升有机与电化学，按周制定刷题与纠错计划。",
            location: "海淀区中关村"
        },
        {
            id: "D-1005",
            title: "六年级语文阅读写作",
            city: "上海",
            district: "浦东",
            grade: "六年级",
            subject: "语文",
            salaryMin: 160,
            salaryMax: 230,
            createdAt: "2026-02-13 21:05",
            description: "希望提升阅读理解与小作文表达，建立稳定输出习惯。",
            location: "浦东新区金桥"
        }
    ];

    const state = {
        rawItems: [],
        visibleItems: [],
        selectedId: null,
        filters: {
            keyword: "",
            city: "全部",
            district: "全部",
            grade: "全部",
            subject: "全部",
            salary: "全部",
            sort: "latest"
        }
    };

    const elements = {
        keyword: document.getElementById("keyword"),
        city: document.getElementById("city"),
        district: document.getElementById("district"),
        grade: document.getElementById("grade"),
        subject: document.getElementById("subject"),
        salary: document.getElementById("salary"),
        sort: document.getElementById("sort"),
        listing: document.getElementById("listing"),
        resultCount: document.getElementById("resultCount"),
        resultHint: document.getElementById("resultHint"),
        emptyState: document.getElementById("emptyState"),
        detailPanel: document.getElementById("detailPanel"),
        detail: document.getElementById("detail"),
        detailPlaceholder: document.getElementById("detailPlaceholder"),
        detailTitle: document.getElementById("detailTitle"),
        detailMeta: document.getElementById("detailMeta"),
        detailLocation: document.getElementById("detailLocation"),
        detailLesson: document.getElementById("detailLesson"),
        detailSalary: document.getElementById("detailSalary"),
        detailCreatedAt: document.getElementById("detailCreatedAt"),
        detailDescription: document.getElementById("detailDescription")
    };

    init().catch(function () {
        elements.resultHint.textContent = "加载失败，已使用本地示例数据。";
    });

    async function init() {
        bindEvents();
        const demandList = await fetchDemands();
        state.rawItems = demandList.map(normalizeDemand);
        hydrateFilterOptions();
        applyFiltersAndRender();
    }

    function bindEvents() {
        elements.keyword.addEventListener("input", function (event) {
            state.filters.keyword = event.target.value.trim();
            applyFiltersAndRender();
        });

        ["city", "district", "grade", "subject", "salary", "sort"].forEach(function (key) {
            elements[key].addEventListener("change", function (event) {
                state.filters[key] = event.target.value;
                if (key === "city") {
                    refreshDistrictOptions();
                }
                applyFiltersAndRender();
            });
        });
    }

    async function fetchDemands() {
        try {
            const response = await fetch(ENDPOINT, { method: "GET" });
            if (!response.ok) {
                throw new Error("API 请求失败");
            }
            const payload = await response.json();

            if (Array.isArray(payload)) {
                elements.resultHint.textContent = `数据来源：${ENDPOINT}`;
                return payload;
            }
            if (payload && payload.success && Array.isArray(payload.data)) {
                elements.resultHint.textContent = `数据来源：${ENDPOINT}`;
                return payload.data;
            }
            throw new Error("接口结构不匹配");
        } catch (error) {
            elements.resultHint.textContent = "数据来源：本地示例（可直接替换为后端 API）";
            return fallbackData;
        }
    }

    function normalizeDemand(item) {
        return {
            id: item.id || item.taskId || cryptoFallbackId(),
            title: item.title || "未命名需求",
            city: item.city || "未知城市",
            district: item.district || "未知区域",
            grade: item.grade || "年级待定",
            subject: item.subject || "科目待定",
            salaryMin: numberOrDefault(item.salaryMin, 0),
            salaryMax: numberOrDefault(item.salaryMax, numberOrDefault(item.salary, 0)),
            createdAt: item.createdAt || item.publishTime || "时间未知",
            description: item.description || item.contentText || "暂无详细说明",
            location: item.location || "地点待沟通"
        };
    }

    function hydrateFilterOptions() {
        fillSelect(elements.city, ["全部"].concat(uniqueByKey(state.rawItems, "city")));
        refreshDistrictOptions();
        fillSelect(elements.grade, ["全部"].concat(uniqueByKey(state.rawItems, "grade")));
        fillSelect(elements.subject, ["全部"].concat(uniqueByKey(state.rawItems, "subject")));
        fillSelect(elements.salary, ["全部", "200 以下", "200-300", "300 以上"]);
    }

    function refreshDistrictOptions() {
        const city = state.filters.city;
        const base = city === "全部" ? state.rawItems : state.rawItems.filter(function (item) {
            return item.city === city;
        });
        const options = ["全部"].concat(uniqueByKey(base, "district"));
        fillSelect(elements.district, options);
        if (!options.includes(state.filters.district)) {
            state.filters.district = "全部";
        }
        elements.district.value = state.filters.district;
    }

    function fillSelect(selectElement, options) {
        const currentValue = selectElement.value;
        selectElement.innerHTML = "";
        options.forEach(function (optionValue) {
            const option = document.createElement("option");
            option.value = optionValue;
            option.textContent = optionValue;
            selectElement.appendChild(option);
        });
        if (options.includes(currentValue)) {
            selectElement.value = currentValue;
        }
    }

    function applyFiltersAndRender() {
        const filtered = state.rawItems.filter(function (item) {
            return matchKeyword(item)
                && matchFilter(item.city, state.filters.city)
                && matchFilter(item.district, state.filters.district)
                && matchFilter(item.grade, state.filters.grade)
                && matchFilter(item.subject, state.filters.subject)
                && matchSalary(item);
        });

        state.visibleItems = sortItems(filtered, state.filters.sort);
        keepSelectionVisible();
        renderList();
        renderDetailBySelection();
        renderMeta();
    }

    function matchKeyword(item) {
        const keyword = state.filters.keyword.toLowerCase();
        if (!keyword) {
            return true;
        }
        const haystack = `${item.title} ${item.subject} ${item.grade} ${item.description}`.toLowerCase();
        return haystack.includes(keyword);
    }

    function matchFilter(value, filterValue) {
        return filterValue === "全部" || value === filterValue;
    }

    function matchSalary(item) {
        const salary = item.salaryMax || item.salaryMin;
        if (state.filters.salary === "全部") {
            return true;
        }
        if (state.filters.salary === "200 以下") {
            return salary < 200;
        }
        if (state.filters.salary === "200-300") {
            return salary >= 200 && salary <= 300;
        }
        return salary > 300;
    }

    function sortItems(list, sortType) {
        const cloned = list.slice();
        if (sortType === "salaryDesc") {
            return cloned.sort(function (a, b) {
                return (b.salaryMax || b.salaryMin) - (a.salaryMax || a.salaryMin);
            });
        }
        if (sortType === "salaryAsc") {
            return cloned.sort(function (a, b) {
                return (a.salaryMin || a.salaryMax) - (b.salaryMin || b.salaryMax);
            });
        }
        return cloned.sort(function (a, b) {
            return dateScore(b.createdAt) - dateScore(a.createdAt);
        });
    }

    function keepSelectionVisible() {
        const hasSelected = state.visibleItems.some(function (item) {
            return item.id === state.selectedId;
        });
        if (!hasSelected) {
            state.selectedId = state.visibleItems.length > 0 ? state.visibleItems[0].id : null;
        }
    }

    function renderList() {
        elements.listing.innerHTML = "";
        elements.emptyState.classList.toggle("hidden", state.visibleItems.length !== 0);

        state.visibleItems.forEach(function (item) {
            const li = document.createElement("li");
            li.className = "card" + (item.id === state.selectedId ? " is-active" : "");
            li.tabIndex = 0;
            li.innerHTML = [
                `<h3 class="card__title">${escapeHtml(item.title)}</h3>`,
                "<div class=\"card__meta\">",
                `<span class=\"chip\">${escapeHtml(item.city)} · ${escapeHtml(item.district)}</span>`,
                `<span>${escapeHtml(item.grade)} / ${escapeHtml(item.subject)}</span>`,
                `<span>¥${item.salaryMin}-${item.salaryMax}/h</span>`,
                "</div>"
            ].join("");

            const activate = function () {
                state.selectedId = item.id;
                renderList();
                renderDetailBySelection();
            };

            li.addEventListener("click", activate);
            li.addEventListener("keydown", function (event) {
                if (event.key === "Enter" || event.key === " ") {
                    event.preventDefault();
                    activate();
                }
            });
            elements.listing.appendChild(li);
        });
    }

    function renderDetailBySelection() {
        const selected = state.visibleItems.find(function (item) {
            return item.id === state.selectedId;
        });

        if (!selected) {
            elements.detail.classList.add("hidden");
            elements.detailPlaceholder.classList.remove("hidden");
            return;
        }

        elements.detailPlaceholder.classList.add("hidden");
        elements.detail.classList.remove("hidden");

        elements.detailTitle.textContent = selected.title;
        elements.detailMeta.textContent = `需求编号：${selected.id}`;
        elements.detailLocation.textContent = `${selected.city}${selected.district} · ${selected.location}`;
        elements.detailLesson.textContent = `${selected.grade} / ${selected.subject}`;
        elements.detailSalary.textContent = `¥${selected.salaryMin} - ¥${selected.salaryMax} / 小时`;
        elements.detailCreatedAt.textContent = selected.createdAt;
        elements.detailDescription.textContent = selected.description;
    }

    function renderMeta() {
        elements.resultCount.textContent = `${state.visibleItems.length} 条需求`;
    }

    function uniqueByKey(list, key) {
        return Array.from(new Set(list.map(function (item) {
            return item[key];
        })));
    }

    function numberOrDefault(value, fallback) {
        const number = Number(value);
        return Number.isFinite(number) ? number : fallback;
    }

    function dateScore(value) {
        const score = Date.parse(value);
        return Number.isFinite(score) ? score : 0;
    }

    function escapeHtml(value) {
        return String(value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#39;");
    }

    function cryptoFallbackId() {
        if (window.crypto && window.crypto.randomUUID) {
            return window.crypto.randomUUID();
        }
        return `fallback-${Date.now()}-${Math.floor(Math.random() * 1000)}`;
    }
})();
