package com.atguigu.wechat.service.impl;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.model.wechat.Menu;
import com.atguigu.vo.wechat.MenuVo;
import com.atguigu.wechat.mapper.MenuMapper;
import com.atguigu.wechat.service.MenuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 菜单 服务实现类
 * </p>
 *
 * @author atguigu
 * @since 2023-03-27
 */
@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    @Autowired
    private WxMpService wxMpService;

    /**
     * 获取全部树形菜单
     * @return
     */
    @Override
    public List<MenuVo> findMenuInfo() {
        List<MenuVo> result = new ArrayList<>();
        //1. 查询所有菜单List集合
        List<Menu> menuList = baseMapper.selectList(null);
        //2. 查询一级菜单 parent_id=0，返回一级菜单list集合
        List<Menu> oneMenuList = menuList.stream()
                .filter(menu -> menu.getParentId().longValue() == 0)
                .collect(Collectors.toList());
        //3. 一级菜单遍历获取每个二级菜单
        for(Menu oneMenu : oneMenuList){
            MenuVo oneMenuVo = new MenuVo();
            BeanUtils.copyProperties(oneMenu, oneMenuVo);//oneMenu转为oneMenuVo
            //4. 获取每个一级菜单里面所有的二级菜单 id 和 parent_id比较
            List<Menu> twoMenuList = menuList.stream()//这个是每个一级菜单oneMenuVo的children属性二级菜单的list
                    .filter(menu -> menu.getParentId().longValue() == oneMenu.getId())
                    .collect(Collectors.toList());

            //5. 将二级菜单作为一级菜单的children，menu转为menuVo对象
            //List<Menu>  twoMenuList ->  List<MenuVo> twoMenuVoList
            List<MenuVo> childeren = new ArrayList<>();
            for (Menu twoMenu : twoMenuList) {
                MenuVo twoMenuVo = new MenuVo();
                BeanUtils.copyProperties(twoMenu, twoMenuVo);
                childeren.add(twoMenuVo);
            }
            oneMenuVo.setChildren(childeren);//把children设置为一级菜单的children
            result.add(oneMenuVo);
        }
        return result;
    }

    /**
     * 同步推送菜单
     */
    @Override
    public void syncMenu() {
        //1. 菜单数据查询出来，封装成微信要求的格式，
        //调用方法，查询到数据List<MenuVo>
        List<MenuVo> menuVoList = this.findMenuInfo();
        //菜单
        JSONArray buttonList = new JSONArray();
        for(MenuVo oneMenuVo : menuVoList) {
            JSONObject one = new JSONObject();
            one.put("name", oneMenuVo.getName());
            if(CollectionUtils.isEmpty(oneMenuVo.getChildren())) {
                one.put("type", oneMenuVo.getType());
                //这个下面#前有/
                one.put("url", "http://workoasecond.vipgz1.91tunnel.com/#"+oneMenuVo.getUrl());
            } else {
                JSONArray subButton = new JSONArray();
                for(MenuVo twoMenuVo : oneMenuVo.getChildren()) {
                    JSONObject view = new JSONObject();
                    view.put("type", twoMenuVo.getType());
                    if(twoMenuVo.getType().equals("view")) {
                        view.put("name", twoMenuVo.getName());
                        //H5页面地址 这个下面#前没有/
                        view.put("url", "http://workoasecond.vipgz1.91tunnel.com#"+twoMenuVo.getUrl());
                    } else {
                        view.put("name", twoMenuVo.getName());
                        view.put("key", twoMenuVo.getMeunKey());
                    }
                    subButton.add(view);
                }
                one.put("sub_button", subButton);
            }
            buttonList.add(one);
        }
        //菜单
        JSONObject button = new JSONObject();
        button.put("button", buttonList);
        //2.调用工具里面的方法实现推送
        try {
            wxMpService.getMenuService().menuCreate(button.toJSONString());
        } catch (WxErrorException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeMenu() {

        try{
            wxMpService.getMenuService().menuDelete();
        } catch (WxErrorException e){
            throw new RuntimeException(e);
        }
    }
}
