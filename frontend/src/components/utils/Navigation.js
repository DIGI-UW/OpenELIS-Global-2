// Navigation utility to handle base path-aware redirects
// This ensures all navigation respects the PUBLIC_URL/basename

export const getBasePath = () => {
    // Get the base path from the PUBLIC_URL environment variable
    // or from the current location if it includes /openelis
    return process.env.PUBLIC_URL || '';
};

export const navigateTo = (path) => {
    if (!path) return;
    const basePath = getBasePath();
    let fullPath = path;
    if (path.startsWith('/')) {
        fullPath = path.startsWith(basePath) ? path : `${basePath}${path}`;
    } else {
        fullPath = `${basePath}/${path}`;
    }
    window.location.href = fullPath;
};

export const assignTo = (path) => {
    if (!path) return;
    const basePath = getBasePath();
    let fullPath = path;
    if (path.startsWith('/')) {
        fullPath = path.startsWith(basePath) ? path : `${basePath}${path}`;
    } else {
        fullPath = `${basePath}/${path}`;
    }
    window.location.assign(fullPath);
};

export const replaceWith = (path) => {
    if (!path) return;
    const basePath = getBasePath();
    let fullPath = path;
    if (path.startsWith('/')) {
        fullPath = path.startsWith(basePath) ? path : `${basePath}${path}`;
    } else {
        fullPath = `${basePath}/${path}`;
    }
    window.location.replace(fullPath);
};

export const getFullPath = (path) => {
    if (!path) return '';
    const basePath = getBasePath();
    if (path.startsWith('/')) {
        return path.startsWith(basePath) ? path : `${basePath}${path}`;
    }
    return `${basePath}/${path}`;
};

export const stripBasePath = (path) => {
    if (!path) return '';
    const basePath = getBasePath();
    if (basePath && path.startsWith(basePath)) {
        return path.substring(basePath.length) || '/';
    }
    return path;
};
