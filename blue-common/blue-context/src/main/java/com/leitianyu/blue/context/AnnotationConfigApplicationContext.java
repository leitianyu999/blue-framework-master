package com.leitianyu.blue.context;

import com.leitianyu.blue.annotation.*;
import com.leitianyu.blue.exception.*;
import com.leitianyu.blue.io.PropertyResolver;
import com.leitianyu.blue.io.ResourceResolver;
import com.leitianyu.blue.utils.ClassUtils;
import com.sun.istack.internal.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author leitianyu
 * @date 2024/1/7
 */
public class AnnotationConfigApplicationContext implements ConfigurableApplicationContext{


    protected final Logger logger = LoggerFactory.getLogger(getClass());
    //配置类
    protected final PropertyResolver propertyResolver;
    //beanList
    protected final Map<String, BeanDefinition> beans;
    //代理类集合
    private List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();
    //已创建的beanName
    private Set<String> creatingBeanNames;

    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        ApplicationContextUtils.setApplicationContext(this);

        this.propertyResolver = propertyResolver;
        // 扫描获取所有Bean的Class类型:
        final Set<String> beanClassNames = scanForClassNames(configClass);

        // 创建bean的定义
        this.beans = createBeanDefinitions(beanClassNames);

        //创建循坏依赖检测
        this.creatingBeanNames = new HashSet<>();

        // 创建@Configuration类型的Bean
        this.beans.values().stream().filter(this::isConfigurationDefinition).sorted().map(def -> {
            createBeanAsEarlySingleton(def);
            return def.getName();
        }).collect(Collectors.toList());

        // 创建BeanPostProcessor类型的Bean:
        List<BeanPostProcessor> processors = this.beans.values().stream()
                // 过滤出BeanPostProcessor:
                .filter(this::isBeanPostProcessorDefinition)
                // 排序:
                .sorted()
                // instantiate and collect:
                .map(def -> (BeanPostProcessor) createBeanAsEarlySingleton(def)).collect(Collectors.toList());
        this.beanPostProcessors.addAll(processors);

        // 创建其他普通Bean:
        createNormalBeans();


        // 通过字段和set方法注入依赖:
        this.beans.values().forEach(this::injectBean);

        // 调用init方法:
        this.beans.values().forEach(this::initBean);

        if (logger.isDebugEnabled()) {
            this.beans.values().stream().sorted().forEach(def -> {
                logger.debug("bean initialized: {}", def);
            });
        }
    }


    protected Set<String> scanForClassNames(Class<?> configClass) {

        //获取扫描包路径
        componentScan scan = ClassUtils.findAnnotation(configClass, componentScan.class);
        final String[] scanPackages = scan == null || scan.value().length == 0 ? new String[] {configClass.getPackage().getName()} : scan.value();
        logger.atInfo().log("component scan in packages: {}", Arrays.toString(scanPackages));

        //逐个扫描
        Set<String> classNameSet = new HashSet<>();
        for (String pkg : scanPackages) {
            // 扫描package:
            logger.atDebug().log("scan package: {}", pkg);
            ResourceResolver resolver = new ResourceResolver(pkg);
            List<String> classList = resolver.scan(res -> {
                String name = res.getName();
                if (name.endsWith(".class")) {
                    return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
                }
                return null;
            });

            if (logger.isDebugEnabled()) {
                classList.forEach((className) -> {
                    logger.debug("class found by component scan: {}", className);
                });
            }

            classNameSet.addAll(classList);
        }

        // 查找@Import(Xyz.class),是否手动import
        Import importConfig = configClass.getAnnotation(Import.class);
        if (importConfig != null) {
            for (Class<?> importConfigClass : importConfig.value()) {
                String className = importConfigClass.getName();
                if (classNameSet.contains(className)) {
                    logger.warn("ignore import: " + className + " for it is already been scanned.");
                } else {
                    logger.debug("class found by import: {}", className);
                    classNameSet.add(className);
                }
            }
        }

        return classNameSet;
    }

    Map<String, BeanDefinition> createBeanDefinitions(Set<String> classNameSet) {

        Map<String, BeanDefinition> defs = new HashMap<>();
        for (String className : classNameSet) {
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new BeanCreationException(e);
            }
            //判断bean类型
            if (clazz.isAnnotation() || clazz.isEnum() || clazz.isInterface()) {
                continue;
            }

            // 是否标注@Component
            Component component = ClassUtils.findAnnotation(clazz, Component.class);
            if (component != null) {
                logger.atDebug().log("found component: {}", clazz.getName());
                //判断clazz类型
                int mod = clazz.getModifiers();
                if (Modifier.isAbstract(mod)) {
                    throw new BeanDefinitionException("@Component class " + clazz.getName() + " must not be abstract.");
                }
                if (Modifier.isPrivate(mod)) {
                    throw new BeanDefinitionException("@Component class " + clazz.getName() + " must not be private.");
                }
                //获取beanName
                String beanName = getBeanName(clazz, component);
                BeanDefinition def = new BeanDefinition(beanName, clazz, getSuitableConstructor(clazz), getOrder(clazz), clazz.isAnnotationPresent(Primary.class),
                        // named init / destroy method:
                        null, null,
                        // init method:
                        ClassUtils.findAnnotationMethod(clazz, PostConstruct.class),
                        // destroy method:
                        ClassUtils.findAnnotationMethod(clazz, PreDestroy.class));
                //存入bean
                addBeanDefinitions(defs, def);
                logger.atDebug().log("define bean: {}", def);

                //处理工厂配置类
                Configuration configuration = ClassUtils.findAnnotation(clazz, Configuration.class);
                if (configuration != null) {
                    //检查是否套上代理注解；不可被代理
                    if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
                        throw new BeanDefinitionException("@Configuration class '" + clazz.getName() + "' cannot be BeanPostProcessor.");
                    }
                    //遍历工厂配置类
                    scanFactoryMethods(beanName, clazz, defs);
                }

            }
        }
        return defs;
    }


    /**
     * 创建一个Bean，然后使用BeanPostProcessor处理，但不进行字段和方法级别的注入。
     * 如果创建的Bean不是Configuration或BeanPostProcessor，则在构造方法中注入的依赖Bean会自动创建。
     *
     * @author leitianyu
     * @date 2024/1/8 19:45
     */
    @Override
    public Object createBeanAsEarlySingleton(BeanDefinition def) {
        logger.atDebug().log("Try create bean '{}' as early singleton: {}", def.getName(), def.getBeanClass().getName());
        //检测是否已创建
        if (!this.creatingBeanNames.add(def.getName())) {
            throw new UnsatisfiedDependencyException(String.format("Circular dependency detected when create bean '%s'", def.getName()));
        }


        Executable createFn;
        if (def.getFactoryName() == null) {
            // 构造方法
            createFn = def.getConstructor();
        } else {
            // 工厂方法
            createFn= def.getFactoryMethod();
        }

        // 参数创建
        final Parameter[] parameters = createFn.getParameters();
        final Annotation[][] parameterAnnos = createFn.getParameterAnnotations();
        // 参数
        Object[] args = new Object[parameters.length];
        // 参数注入
        for (int i = 0; i < parameters.length; i++) {
            final Parameter para = parameters[i];
            final Annotation[] annos = parameterAnnos[i];
            // 参数注入
            final Value value = ClassUtils.getAnnotation(annos, Value.class);
            // bean注入
            final Autowired autowired = ClassUtils.getAnnotation(annos, Autowired.class);

            // @Configuration类型的Bean是工厂，不允许使用@Autowired创建:
            final boolean isConfiguration = isConfigurationDefinition(def);
            if (isConfiguration && autowired != null) {
                throw new BeanCreationException(
                        String.format("Cannot specify @Autowired when create @Configuration bean '%s': %s.", def.getName(), def.getBeanClass().getName()));
            }

            // BeanPostProcessor不能依赖其他Bean，不允许使用@Autowired创建:
            final boolean isBeanPostProcessor = isBeanPostProcessorDefinition(def);
            if (isBeanPostProcessor && autowired != null) {
                throw new BeanCreationException(
                        String.format("Cannot specify @Autowired when create BeanPostProcessor '%s': %s.", def.getName(), def.getBeanClass().getName()));
            }

            // 参数需要@Value或@Autowired两者之一:
            if (value != null && autowired != null) {
                throw new BeanCreationException(
                        String.format("Cannot specify both @Autowired and @Value when create bean '%s': %s.", def.getName(), def.getBeanClass().getName()));
            }
            if (value == null && autowired == null) {
                throw new BeanCreationException(
                        String.format("Must specify @Autowired or @Value when create bean '%s': %s.", def.getName(), def.getBeanClass().getName()));
            }

            final Class<?> type = para.getType();
            if (value != null) {
                // 参数是@Value注入
                args[i] = this.propertyResolver.getRequiredProperty(value.value(), type);
            } else {
                // 参数是@Autowired注入
                String name = autowired.name();
                boolean required = autowired.value();
                // 获取bean
                BeanDefinition dependOnDef = name.isEmpty() ? findBeanDefinition(type) : findBeanDefinition(name);
                // 检测required==true?
                if (required && dependOnDef == null) {
                    throw new BeanCreationException(String.format("Missing autowired bean with type '%s' when create bean '%s': %s.", type.getName(),
                            def.getName(), def.getBeanClass().getName()));
                }

                if (dependOnDef != null) {
                    //获取实例
                    Object dependOnDefInstance = dependOnDef.getInstance();
                    if (dependOnDefInstance == null && !isConfiguration && !isBeanPostProcessor) {
                        // 当前依赖Bean尚未初始化，递归调用初始化该依赖Bean:
                        dependOnDefInstance = createBeanAsEarlySingleton(dependOnDef);
                    }
                    args[i] = dependOnDefInstance;
                } else {
                    args[i] = null;
                }

            }
        }


        // 创建实例
        Object instance;
        if (def.getFactoryName() == null) {
            try {
                instance = def.getConstructor().newInstance(args);
            } catch (Exception e) {
                throw new BeanCreationException(String.format("Exception when create bean '%s': %s", def.getName(), def.getBeanClass().getName()), e);
            }
        } else {
            // 用@Bean方法创建:
            Object configInstance = getBean(def.getFactoryName());
            try {
                instance = def.getFactoryMethod().invoke(configInstance, args);
            } catch (Exception e) {
                throw new BeanCreationException(String.format("Exception when create bean '%s': %s", def.getName(), def.getBeanClass().getName()), e);
            }
        }
        def.setInstance(instance);

        // 调用BeanPostProcessor处理Bean
        for (BeanPostProcessor processor : beanPostProcessors) {
            Object processed = processor.postProcessBeforeInitialization(def.getInstance(), def.getName());
            if (processed == null) {
                throw new BeanCreationException(String.format("PostBeanProcessor returns null when process bean '%s' by %s", def.getName(), processor));
            }
            // 如果一个BeanPostProcessor替换了原始Bean，则更新Bean的引用:
            if (def.getInstance() != processed) {
                logger.atDebug().log("Bean '{}' was replaced by post processor {}.", def.getName(), processor.getClass().getName());
                def.setInstance(processed);
            }
        }
        return def.getInstance();
    }

    void createNormalBeans() {
        // 获取BeanDefinition列表:
        List<BeanDefinition> defs = this.beans.values().stream()
                // filter bean definitions by not instantiation:
                .filter(def -> def.getInstance() == null).sorted().collect(Collectors.toList());
        defs.forEach(def -> {
            // 如果Bean未被创建(可能在其他Bean的构造方法注入前被创建):
            if (def.getInstance() == null) {
                // 创建Bean:
                createBeanAsEarlySingleton(def);
            }
        });
    }


    /**
     * 注入依赖但不调用init方法
     */
    void injectBean(BeanDefinition def) {
        // 获取Bean实例，或被代理的原始实例
        final Object beanInstance = getProxiedInstance(def);
        try {
            injectProperties(def, def.getBeanClass(), beanInstance);
        } catch (ReflectiveOperationException e) {
            throw new BeanCreationException(e);
        }
    }


    void injectProperties(BeanDefinition def, Class<?> clazz, Object bean) throws ReflectiveOperationException {

        // 在当前类查找Field和Method并注入:
        // 字段注入
        for (Field f : clazz.getDeclaredFields()) {
            tryInjectProperties(def, clazz, bean, f);
        }
        // Setter方法注入
        for (Method m : clazz.getDeclaredMethods()) {
            tryInjectProperties(def, clazz, bean, m);
        }
        // 在父类查找Field和Method并注入:
        Class<?> superClazz = clazz.getSuperclass();
        if (superClazz != null) {
            injectProperties(def, superClazz, bean);
        }
    }
    
    /**
     * Setter方法注入
     * 字段注入
     * 
     * @author leitianyu
     * @date 2024/1/9 14:56
     */
    void tryInjectProperties(BeanDefinition def, Class<?> clazz, Object bean, AccessibleObject acc) throws ReflectiveOperationException {
        Value value = acc.getAnnotation(Value.class);
        Autowired autowired = acc.getAnnotation(Autowired.class);
        if (value == null && autowired == null) {
            return;
        }

        Field field = null;
        Method method = null;
        if (acc instanceof Field) {
            Field f = (Field)acc;
            checkFieldOrMethod(f);
            f.setAccessible(true);
            field = f;
        }
        if (acc instanceof Method) {
            Method m = (Method) acc;
            checkFieldOrMethod(m);
            // 暂时只支持单一注入
            if (m.getParameters().length != 1) {
                throw new BeanDefinitionException(
                        String.format("Cannot inject a non-setter method %s for bean '%s': %s", m.getName(), def.getName(), def.getBeanClass().getName()));
            }
            m.setAccessible(true);
            method = m;
        }

        String accessibleName = field != null ? field.getName() : method.getName();
        Class<?> accessibleType = field != null ? field.getType() : method.getParameterTypes()[0];

        if (value != null && autowired != null) {
            throw new BeanCreationException(String.format("Cannot specify both @Autowired and @Value when inject %s.%s for bean '%s': %s",
                    clazz.getSimpleName(), accessibleName, def.getName(), def.getBeanClass().getName()));
        }

        if (value != null) {
            Object property = this.propertyResolver.getRequiredProperty(value.value(), accessibleType);
            if (field != null) {
                logger.atDebug().log("Field injection: {}.{} = {}", def.getBeanClass().getName(), accessibleName, property);
                field.set(bean, property);
            }
            if (method != null) {
                logger.atDebug().log("Method injection: {}.{} ({})", def.getBeanClass().getName(), accessibleName, property);
                method.invoke(bean, property);
            }
        }

        // @Autowired注入:
        if (autowired != null) {
            String name = autowired.name();
            boolean required = autowired.value();
            Object depends = name.isEmpty() ? findBean(accessibleType) : findBean(name, accessibleType);
            if (required && depends == null) {
                throw new UnsatisfiedDependencyException(String.format("Dependency bean not found when inject %s.%s for bean '%s': %s", clazz.getSimpleName(),
                        accessibleName, def.getName(), def.getBeanClass().getName()));
            }
            if (depends != null) {
                if (field != null) {
                    logger.atDebug().log("Field injection: {}.{} = {}", def.getBeanClass().getName(), accessibleName, depends);
                    field.set(bean, depends);
                }
                if (method != null) {
                    logger.atDebug().log("Mield injection: {}.{} ({})", def.getBeanClass().getName(), accessibleName, depends);
                    method.invoke(bean, depends);
                }
            }
        }
    }

    void initBean(BeanDefinition def) {
        // 获取Bean实例，或被代理的原始实例:
        final Object beanInstance = getProxiedInstance(def);
        // 调用init方法:
        callMethod(beanInstance, def.getInitMethod(), def.getInitMethodName());

        // 调用BeanPostProcessor.postProcessAfterInitialization():
        beanPostProcessors.forEach(beanPostProcessor -> {
            Object processedInstance = beanPostProcessor.postProcessAfterInitialization(def.getInstance(), def.getName());
            if (processedInstance != def.getInstance()) {
                logger.atDebug().log("BeanPostProcessor {} return different bean from {} to {}.", beanPostProcessor.getClass().getSimpleName(),
                        def.getInstance().getClass().getName(), processedInstance.getClass().getName());
                def.setInstance(processedInstance);
            }
        });
    }

    private void callMethod(Object beanInstance, Method method, String namedMethod) {
        // 调用init/destroy方法:
        if (method != null) {
            try {
                method.invoke(beanInstance);
            } catch (ReflectiveOperationException e) {
                throw new BeanCreationException(e);
            }
        } else if (namedMethod != null) {
            // 查找initMethod/destroyMethod="xyz"，注意是在实际类型中查找:
            Method named = ClassUtils.getNamedMethod(beanInstance.getClass(), namedMethod);
            named.setAccessible(true);
            try {
                named.invoke(beanInstance);
            } catch (ReflectiveOperationException e) {
                throw new BeanCreationException(e);
            }
        }
    }


    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> T findBean(String name, Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(name, requiredType);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> T findBean(Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if (def == null) {
            return null;
        }
        return (T) def.getRequiredInstance();
    }


    void checkFieldOrMethod(Member m) {
        int mod = m.getModifiers();
        if (Modifier.isStatic(mod)) {
            throw new BeanDefinitionException("Cannot inject static field: " + m);
        }
        if (Modifier.isFinal(mod)) {
            if (m instanceof Field) {
                throw new BeanDefinitionException("Cannot inject final field: ");
            }
            if (m instanceof Method) {
                logger.warn(
                        "Inject final method should be careful because it is not called on target bean when bean is proxied and may cause NullPointerException.");
            }
        }
    }


    /**
     * 获取Bean实例，或被代理的原始实例
     * 
     * @author leitianyu
     * @date 2024/1/9 14:55
     */
    private Object getProxiedInstance(BeanDefinition def) {
        Object instance = def.getInstance();
        // 如果Proxy改变了原始Bean，又希望注入到原始Bean，则由BeanPostProcessor指定原始Bean:
        List<BeanPostProcessor> reversedBeanPostProcessors = new ArrayList<>(this.beanPostProcessors);
        //降序
        Collections.reverse(reversedBeanPostProcessors);
        for (BeanPostProcessor beanPostProcessor : reversedBeanPostProcessors) {
            Object restoredInstance = beanPostProcessor.postProcessOnSetProperty(instance, def.getName());
            if (restoredInstance != instance) {
                logger.atDebug().log("BeanPostProcessor {} specified injection from {} to {}.", beanPostProcessor.getClass().getSimpleName(),
                        instance.getClass().getSimpleName(), restoredInstance.getClass().getSimpleName());
                instance = restoredInstance;
            }
        }
        return instance;
    }


    boolean isBeanPostProcessorDefinition(BeanDefinition def) {
        return BeanPostProcessor.class.isAssignableFrom(def.getBeanClass());
    }


    boolean isConfigurationDefinition(BeanDefinition def) {
        return ClassUtils.findAnnotation(def.getBeanClass(), Configuration.class) != null;
    }

    /**
     * 扫描方法是否含有Bean注解并定义bean
     *
     * @author leitianyu
     * @date 2024/1/8 19:11
     */
    void scanFactoryMethods(String factoryBeanName, Class<?> clazz, Map<String, BeanDefinition> defs) {
        //获取所有方法
        for (Method method : clazz.getDeclaredMethods()) {
            Bean bean = method.getAnnotation(Bean.class);
            if (bean != null) {
                //检查方法的修饰
                int mod = method.getModifiers();
                if (Modifier.isAbstract(mod)) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be abstract.");
                }
                if (Modifier.isFinal(mod)) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be final.");
                }
                if (Modifier.isPrivate(mod)) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not be private.");
                }
                //检查返回对象的类型
                Class<?> beanClass = method.getReturnType();
                //不可以是基本数据类型
                if (beanClass.isPrimitive()) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not return primitive type.");
                }
                //不可以是void
                if (beanClass == void.class || beanClass == Void.class) {
                    throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + " must not return void.");
                }

                BeanDefinition def = new BeanDefinition(ClassUtils.getBeanName(method), beanClass, factoryBeanName, method, getOrder(method),
                        method.isAnnotationPresent(Primary.class),
                        bean.initMethod().isEmpty() ? null : bean.initMethod(),
                        bean.destroyMethod().isEmpty() ? null : bean.destroyMethod(),
                        null, null);
                addBeanDefinitions(defs, def);
                logger.atDebug().log("define bean: {}", def);
            }
        }
    }




    void addBeanDefinitions(Map<String, BeanDefinition> defs, BeanDefinition def) {
        if (defs.put(def.getName(), def) != null) {
            throw new BeanDefinitionException("Duplicate bean name: " + def.getName());
        }
    }


    /**
     * 获取构造方法
     *
     * @author leitianyu
     * @date 2024/1/8 18:46
     */
    Constructor<?> getSuitableConstructor(Class<?> clazz) {
        //获取public修饰的构造器
        Constructor<?>[] constructors = clazz.getConstructors();
        if (constructors.length == 0) {
            //获取全部的构造器
            constructors = clazz.getDeclaredConstructors();
            if (constructors.length != 1) {
                throw new BeanDefinitionException("More than one constructor found in class " + clazz.getName() + ".");
            }
        }
        if (constructors.length != 1) {
            throw new BeanDefinitionException("More than one public constructor found in class " + clazz.getName() + ".");
        }
        return constructors[0];
    }

    /**
     * 获取Order值
     *
     * @author leitianyu
     * @date 2024/1/8 18:51
     */
    int getOrder(Class<?> clazz) {
        Order order = clazz.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    /**
     * 获取Order值
     *
     * @author leitianyu
     * @date 2024/1/9 15:34
     */
    int getOrder(Method method) {
        Order order = method.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }


    /**
     * getBeanName
     *
     * @author leitianyu
     * @date 2024/1/8 18:51
     */
    public static String getBeanName(Class<?> clazz, Component anno) {
        String name;
        name = anno.value();
        if (name.isEmpty()) {
            // default name: "HelloWorld" => "helloWorld"
            name = clazz.getSimpleName();
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }


    @Override
    public boolean containsBean(String name) {
        return this.beans.containsKey(name);
    }

    /**
     * 通过name查找Bean
     *
     * @author leitianyu
     * @date 2024/1/8 20:15
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        BeanDefinition def = this.beans.get(name);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with name '%s'.", name));
        }
        return (T) def.getRequiredInstance();
    }

    /**
     * 通过Name和Type查找Bean
     *
     * @author leitianyu
     * @date 2024/1/9 15:37
     */
    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        T t = findBean(name, requiredType);
        if (t == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with name '%s' and type '%s'.", name, requiredType));
        }
        return t;
    }

    /**
     * 通过Type查找Bean
     *
     * @author leitianyu
     * @date 2024/1/9 15:39
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> requiredType) {
        BeanDefinition def = findBeanDefinition(requiredType);
        if (def == null) {
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with type '%s'.", requiredType));
        }
        return (T) def.getRequiredInstance();
    }

    /**
     * 通过Type查找Beans
     *
     * @author leitianyu
     * @date 2024/1/9 15:39
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getBeans(Class<T> requiredType) {
        List<BeanDefinition> defs = findBeanDefinitions(requiredType);
        if (defs.isEmpty()) {
            return new ArrayList<>();
        }
        List<T> list = new ArrayList<>(defs.size());
        for (BeanDefinition def : defs) {
            list.add((T) def.getRequiredInstance());
        }
        return list;
    }

    @Override
    public void close() {
        logger.info("Closing {}...", this.getClass().getName());
        this.beans.values().forEach(def -> {
            final Object beanInstance = getProxiedInstance(def);
            callMethod(beanInstance, def.getDestroyMethod(), def.getDestroyMethodName());
        });
        this.beans.clear();
        logger.info("{} closed.", this.getClass().getName());
        ApplicationContextUtils.setApplicationContext(null);
    }

    /**
     * 根据Type查找若干个BeanDefinition，返回0个或多个。
     *
     * @author leitianyu
     * @date 2024/1/9 15:11
     */
    @Override
    public List<BeanDefinition> findBeanDefinitions(Class<?> type) {
        return this.beans.values().stream().filter(def -> type.isAssignableFrom(def.getBeanClass())).sorted().collect(Collectors.toList());
    }


    /**
     * 根据Type查找某个BeanDefinition
     * 如果不存在返回null，如果存在多个返回@Primary标注的一个
     * 如果有多个@Primary标注，或没有@Primary标注但找到多个，均抛出NoUniqueBeanDefinitionException
     *
     * @author leitianyu
     * @date 2024/1/9 15:12
     */
    @Nullable
    @Override
    public BeanDefinition findBeanDefinition(Class<?> type) {
        List<BeanDefinition> defs = findBeanDefinitions(type);
        if (defs.isEmpty()) {
            return null;
        }
        if (defs.size() == 1) {
            return defs.get(0);
        }
        List<BeanDefinition> collect = defs.stream().filter(def -> def.isPrimary()).collect(Collectors.toList());
        if (collect.size() == 1) {
            return collect.get(0);
        }
        if (collect.isEmpty()) {
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, but no @Primary specified.", type.getName()));
        } else {
            throw new NoUniqueBeanDefinitionException(String.format("Multiple bean with type '%s' found, and multiple @Primary specified.", type.getName()));
        }
    }

    /**
     * 根据Name查找BeanDefinition，如果Name不存在，返回null
     *
     * @author leitianyu
     * @date 2024/1/9 15:17
     */
    @Nullable
    @Override
    public BeanDefinition findBeanDefinition(String name) {
        return this.beans.get(name);
    }


    /**
     * 根据Name和Type查找BeanDefinition，如果Name不存在，返回null，如果Name存在，但Type不匹配，抛出异常。
     *
     * @author leitianyu
     * @date 2024/1/9 15:16
     */
    @Nullable
    @Override
    public BeanDefinition findBeanDefinition(String name, Class<?> requiredType) {
        BeanDefinition def = findBeanDefinition(name);
        if (def == null) {
            return null;
        }
        if (!requiredType.isAssignableFrom(def.getBeanClass())) {
            throw new BeanNotOfRequiredTypeException(String.format("Autowire required type '%s' but bean '%s' has actual type '%s'.", requiredType.getName(),
                    name, def.getBeanClass().getName()));
        }
        return def;
    }


}
