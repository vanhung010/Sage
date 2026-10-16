import { Navigate, Route, Routes,} from "react-router-dom";

import { HomePage } from "../pages/Home";
import { MenuPage } from "../pages/Menu";
import { LoginPage } from "../pages/Login";
import { RegisterPage } from "../pages/Register";
import { DishDetailPage } from "../pages/DishDetail/DishDetailPage";

export function AppRouter() {
    return (
        <Routes>
            <Route path = "/" element = {<HomePage></HomePage>}></Route>
            <Route path = "/menu" element = {<MenuPage></MenuPage>}></Route>
            <Route path = "/login" element = {<LoginPage></LoginPage>}></Route>
            <Route path = "/register" element = {<RegisterPage></RegisterPage>}></Route>
            <Route path="/menu/:id" element={<DishDetailPage />} />
            <Route path="*" element={<Navigate to="/" replace />}
      />
        </Routes>
    )
}