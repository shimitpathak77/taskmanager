const state = {
    token: localStorage.getItem("token"),
    user: JSON.parse(localStorage.getItem("user") || "null"),
    projects: [],
    selectedProjectId: null
};

const authView = document.getElementById("authView");
const appView = document.getElementById("appView");
const authMessage = document.getElementById("authMessage");
const appMessage = document.getElementById("appMessage");

const elements = {
    loginTab: document.getElementById("loginTab"),
    signupTab: document.getElementById("signupTab"),
    loginForm: document.getElementById("loginForm"),
    signupForm: document.getElementById("signupForm"),
    currentUser: document.getElementById("currentUser"),
    projectList: document.getElementById("projectList"),
    memberList: document.getElementById("memberList"),
    taskList: document.getElementById("taskList"),
    myTaskList: document.getElementById("myTaskList"),
    selectedProjectName: document.getElementById("selectedProjectName"),
    selectedProjectDescription: document.getElementById("selectedProjectDescription"),
    projectTools: document.getElementById("projectTools")
};

function setMessage(target, text, isSuccess = false) {
    target.textContent = text || "";
    target.style.color = isSuccess ? "#027a48" : "#b42318";
}

function showAuthTab(tab) {
    const isLogin = tab === "login";
    elements.loginTab.classList.toggle("active", isLogin);
    elements.signupTab.classList.toggle("active", !isLogin);
    elements.loginForm.classList.toggle("hidden", !isLogin);
    elements.signupForm.classList.toggle("hidden", isLogin);
    setMessage(authMessage, "");
}

async function api(path, options = {}) {
    const headers = {
        "Content-Type": "application/json",
        ...options.headers
    };

    if (state.token) {
        headers.Authorization = `Bearer ${state.token}`;
    }

    const response = await fetch(path, {
        ...options,
        headers
    });

    const text = await response.text();
    const data = text ? JSON.parse(text) : null;

    if (!response.ok) {
        const message = data?.error || Object.values(data || {})[0] || "Request failed";
        throw new Error(message);
    }

    return data;
}

function saveSession(auth) {
    state.token = auth.token;
    state.user = {
        name: auth.name,
        email: auth.email,
        role: auth.role
    };
    localStorage.setItem("token", state.token);
    localStorage.setItem("user", JSON.stringify(state.user));
}

function clearSession() {
    state.token = null;
    state.user = null;
    state.projects = [];
    state.selectedProjectId = null;
    localStorage.removeItem("token");
    localStorage.removeItem("user");
}

function renderShell() {
    const loggedIn = Boolean(state.token);
    authView.classList.toggle("hidden", loggedIn);
    appView.classList.toggle("hidden", !loggedIn);

    if (loggedIn) {
        elements.currentUser.textContent = `${state.user?.name || "User"} (${state.user?.email || ""})`;
    }
}

async function loadAll() {
    if (!state.token) return;

    try {
        const [projects, dashboard, myTasks] = await Promise.all([
            api("/api/projects"),
            api("/api/tasks/dashboard"),
            api("/api/tasks/my")
        ]);

        state.projects = projects || [];
        renderProjects();
        renderDashboard(dashboard || {});
        renderMyTasks(myTasks || []);

        if (state.selectedProjectId) {
            await selectProject(state.selectedProjectId, false);
        }
    } catch (error) {
        setMessage(appMessage, error.message);
    }
}

function renderDashboard(stats) {
    document.getElementById("totalCount").textContent = stats.total || 0;
    document.getElementById("todoCount").textContent = stats.todo || 0;
    document.getElementById("inProgressCount").textContent = stats.inProgress || 0;
    document.getElementById("doneCount").textContent = stats.done || 0;
    document.getElementById("overdueCount").textContent = stats.overdue || 0;
}

function renderProjects() {
    elements.projectList.innerHTML = "";

    if (state.projects.length === 0) {
        elements.projectList.innerHTML = `<p class="muted">No projects yet.</p>`;
        return;
    }

    state.projects.forEach((project) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = `project-item ${project.id === state.selectedProjectId ? "active" : ""}`;
        button.innerHTML = `
            <strong>${escapeHtml(project.name)}</strong>
            <span>${escapeHtml(project.description || "No description")}</span>
        `;
        button.addEventListener("click", () => selectProject(project.id));
        elements.projectList.appendChild(button);
    });
}

async function selectProject(projectId, showMessage = true) {
    try {
        state.selectedProjectId = projectId;
        const project = state.projects.find((item) => item.id === projectId) || await api(`/api/projects/${projectId}`);
        const [members, tasks] = await Promise.all([
            api(`/api/projects/${projectId}/members`),
            api(`/api/tasks/project/${projectId}`)
        ]);

        elements.selectedProjectName.textContent = project.name;
        elements.selectedProjectDescription.textContent = project.description || "";
        elements.projectTools.classList.remove("hidden");
        renderProjects();
        renderMembers(members || []);
        renderProjectTasks(tasks || []);

        if (showMessage) setMessage(appMessage, "");
    } catch (error) {
        setMessage(appMessage, error.message);
    }
}

function renderMembers(members) {
    elements.memberList.innerHTML = "";

    if (members.length === 0) {
        elements.memberList.innerHTML = `<p class="muted">No members found.</p>`;
        return;
    }

    members.forEach((member) => {
        const item = document.createElement("div");
        item.className = "member-item";
        item.innerHTML = `
            <strong>${escapeHtml(member.user?.name || "User")}</strong>
            <div class="muted">${escapeHtml(member.user?.email || "")}</div>
            <div class="muted">${escapeHtml(member.role || "MEMBER")}</div>
        `;
        elements.memberList.appendChild(item);
    });
}

function renderProjectTasks(tasks) {
    elements.taskList.innerHTML = "";

    if (tasks.length === 0) {
        elements.taskList.innerHTML = `<p class="muted">No tasks found.</p>`;
        return;
    }

    tasks.forEach((task) => elements.taskList.appendChild(createTaskItem(task, true)));
}

function renderMyTasks(tasks) {
    elements.myTaskList.innerHTML = "";

    if (tasks.length === 0) {
        elements.myTaskList.innerHTML = `<p class="muted">No assigned tasks.</p>`;
        return;
    }

    tasks.forEach((task) => elements.myTaskList.appendChild(createTaskItem(task, false)));
}

function createTaskItem(task, compact) {
    const item = document.createElement("div");
    item.className = "task-item";

    item.innerHTML = `
        <strong>${escapeHtml(task.title)}</strong>
        <p class="muted">${escapeHtml(task.description || "No description")}</p>
        <div class="task-meta">
            <span>${escapeHtml(task.status)}</span>
            <span>${escapeHtml(task.dueDate || "No due date")}</span>
            <span>${escapeHtml(task.assignee?.email || "Unassigned")}</span>
        </div>
        <div class="status-row">
            <select aria-label="Task status">
                <option value="TODO">Todo</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="DONE">Done</option>
            </select>
            <button class="secondary-btn" type="button">Update</button>
        </div>
    `;

    const select = item.querySelector("select");
    const button = item.querySelector("button");
    select.value = task.status;
    button.addEventListener("click", () => updateTaskStatus(task.id, select.value));

    if (compact) {
        item.querySelector(".status-row").classList.add("hidden");
    }

    return item;
}

async function updateTaskStatus(taskId, status) {
    try {
        await api(`/api/tasks/${taskId}/status`, {
            method: "PUT",
            body: JSON.stringify({ status })
        });
        await loadAll();
        setMessage(appMessage, "Task status updated.", true);
    } catch (error) {
        setMessage(appMessage, error.message);
    }
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

elements.loginTab.addEventListener("click", () => showAuthTab("login"));
elements.signupTab.addEventListener("click", () => showAuthTab("signup"));

elements.loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
        const auth = await api("/api/auth/login", {
            method: "POST",
            body: JSON.stringify({
                email: document.getElementById("loginEmail").value,
                password: document.getElementById("loginPassword").value
            })
        });
        saveSession(auth);
        renderShell();
        await loadAll();
    } catch (error) {
        setMessage(authMessage, error.message);
    }
});

elements.signupForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
        const auth = await api("/api/auth/signup", {
            method: "POST",
            body: JSON.stringify({
                name: document.getElementById("signupName").value,
                email: document.getElementById("signupEmail").value,
                password: document.getElementById("signupPassword").value
            })
        });
        saveSession(auth);
        renderShell();
        await loadAll();
    } catch (error) {
        setMessage(authMessage, error.message);
    }
});

document.getElementById("logoutBtn").addEventListener("click", () => {
    clearSession();
    renderShell();
});

document.getElementById("refreshBtn").addEventListener("click", loadAll);

document.getElementById("projectForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
        const project = await api("/api/projects", {
            method: "POST",
            body: JSON.stringify({
                name: document.getElementById("projectName").value,
                description: document.getElementById("projectDescription").value
            })
        });
        event.target.reset();
        await loadAll();
        await selectProject(project.id);
        setMessage(appMessage, "Project created.", true);
    } catch (error) {
        setMessage(appMessage, error.message);
    }
});

document.getElementById("memberForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!state.selectedProjectId) return;

    try {
        await api(`/api/projects/${state.selectedProjectId}/members`, {
            method: "POST",
            body: JSON.stringify({
                email: document.getElementById("memberEmail").value,
                role: document.getElementById("memberRole").value
            })
        });
        event.target.reset();
        await selectProject(state.selectedProjectId);
        setMessage(appMessage, "Member added.", true);
    } catch (error) {
        setMessage(appMessage, error.message);
    }
});

document.getElementById("taskForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!state.selectedProjectId) return;

    const assigneeEmail = document.getElementById("taskAssignee").value.trim();
    const dueDate = document.getElementById("taskDueDate").value;

    try {
        await api("/api/tasks", {
            method: "POST",
            body: JSON.stringify({
                projectId: String(state.selectedProjectId),
                title: document.getElementById("taskTitle").value,
                description: document.getElementById("taskDescription").value,
                assigneeEmail: assigneeEmail || null,
                dueDate: dueDate || null
            })
        });
        event.target.reset();
        await loadAll();
        setMessage(appMessage, "Task created.", true);
    } catch (error) {
        setMessage(appMessage, error.message);
    }
});

renderShell();
loadAll();
