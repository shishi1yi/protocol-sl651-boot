/*
 *      Copyright (c) 2018-2028, Chill Zhuang All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in the
 *  documentation and/or other materials provided with the distribution.
 *  Neither the name of the dreamlu.net developer nor the names of its
 *  contributors may be used to endorse or promote products derived from
 *  this software without specific prior written permission.
 *  Author: Chill 庄骞 (smallchill@163.com)
 */
package cc.shaoyi.sl651.modules.protocol.config;



import cc.shaoyi.sl651.modules.protocol.props.PropertyLimitProperties;
import cc.shaoyi.sl651.modules.protocol.props.Sl651NettyContentProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 配置feign、mybatis包名、properties
 *
 * @author Chill
 */
@Configuration
@ComponentScan({"cc.shaoyi.sl651.modules.protocol", "cn.hutool.extra.spring"})
@EnableConfigurationProperties({
	Sl651NettyContentProperties.class,
	PropertyLimitProperties.class
})
public class Sl651Configuration {

}
