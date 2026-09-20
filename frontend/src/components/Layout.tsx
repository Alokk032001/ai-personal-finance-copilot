import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export default function Layout() {
  const { user, logout } = useAuth();
  return (
    <div className="shell">
      <aside className="sidebar">
        <h1 className="brand">AI Finance Copilot</h1>
        <nav className="nav">
          <NavLink to="/dashboard">Dashboard</NavLink>
          <NavLink to="/upload">Upload</NavLink>
          <NavLink to="/bills">Bills</NavLink>
        </nav>
        <p className="muted" style={{ marginTop: "2rem", fontSize: "0.85rem" }}>{user?.email}</p>
        <button type="button" onClick={logout} style={{ marginTop: "0.5rem" }} className="btn secondary">
          Log out
        </button>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
