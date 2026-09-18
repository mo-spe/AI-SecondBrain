import { BookOpen, BrainCircuit, ClipboardCheck, Globe, Globe2, LayoutDashboard, LogOut, MessageCircle, Mic2, Network, Settings2 } from 'lucide-react';
import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';
import { preloadRoute } from '../lib/routes.js';

const navItems = [
  { to: '/', label: '学习概览', icon: LayoutDashboard },
  { to: '/knowledge', label: '我的知识', icon: BookOpen },
  { to: '/feynman', label: '费曼复述', icon: Mic2 },
  { to: '/quiz', label: '智能测验', icon: ClipboardCheck },
  { to: '/rag', label: '知识问答', icon: MessageCircle },
  { to: '/graph', label: '知识图谱', icon: Network },
  { to: '/universe', label: '知识宇宙', icon: Globe2 },
  { to: '/earth', label: '数字地球', icon: Globe }
];

const primaryNavItems = navItems.slice(0, 5);
const exploreNavItems = navItems.slice(5);

function NavigationItem({ item }) {
  const { to, label, icon: Icon } = item;
  return (
    <NavLink
      to={to}
      end={to === '/'}
      onMouseEnter={() => preloadRoute(to)}
      onFocus={() => preloadRoute(to)}
      onTouchStart={() => preloadRoute(to)}
      className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
    >
      <Icon size={19} strokeWidth={1.9} />
      <span>{label}</span>
    </NavLink>
  );
}

export default function AppShell() {
  const { user, signOut } = useAuth();

  return (
    <div className="app-layout">
      <a className="skip-link" href="#main-content">跳转到主要内容</a>
      <aside className="sidebar">
        <div className="brand-lockup">
          <div className="brand-mark"><BrainCircuit size={20} strokeWidth={2.2} /></div>
          <div>
            <strong>Feynman</strong>
            <span>学习工作台</span>
          </div>
        </div>
        <div className="sidebar-label">工作区</div>
        <nav className="main-nav" aria-label="主要导航">
          <div className="nav-group">
            <span className="nav-group-label">学习</span>
            {primaryNavItems.map((item) => <NavigationItem item={item} key={item.to} />)}
          </div>
          <div className="nav-group nav-group-explore">
            <span className="nav-group-label">探索</span>
            {exploreNavItems.map((item) => <NavigationItem item={item} key={item.to} />)}
          </div>
        </nav>
        <div className="sidebar-bottom">
          <div className="nav-item is-muted"><Settings2 size={19} /><span>设置</span></div>
          <button className="account-row" onClick={signOut} type="button">
            <span className="avatar">{user?.username?.slice(0, 1).toUpperCase()}</span>
            <span className="account-copy"><strong>{user?.username}</strong><small>退出登录</small></span>
            <LogOut size={16} />
          </button>
        </div>
      </aside>
      <div className="content-shell">
        <header className="mobile-topbar">
          <div className="brand-lockup">
            <div className="brand-mark"><BrainCircuit size={18} strokeWidth={2.2} /></div>
            <div><strong>Feynman</strong><span>学习工作台</span></div>
          </div>
          <span className="mobile-user-mark" aria-label={`当前用户 ${user?.username}`}>{user?.username?.slice(0, 1).toUpperCase()}</span>
        </header>
        <main className="main-content" id="main-content" tabIndex="-1"><Outlet /></main>
        <nav className="mobile-nav" aria-label="主要导航">
          {primaryNavItems.map((item) => <NavigationItem item={item} key={item.to} />)}
        </nav>
      </div>
    </div>
  );
}
