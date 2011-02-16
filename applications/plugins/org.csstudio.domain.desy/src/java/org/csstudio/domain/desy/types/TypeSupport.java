///*
// * Copyright (c) 2010 Stiftung Deutsches Elektronen-Synchrotron,
// * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
// *
// * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS.
// * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
// * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND
// * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
// * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
// * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
// * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE
// * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR
// * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE.
// * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
// * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS,
// * OR MODIFICATIONS.
// * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION,
// * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS
// * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY
// * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
// */
//package org.csstudio.domain.desy.types;
//
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import javax.annotation.CheckForNull;
//import javax.annotation.Nonnull;
//
//import org.eclipse.core.runtime.Platform;
//
///**
// * Type support after carcassi's pattern: {@link org.epics.pvmanager.TypeSupport}
// *
// * @author carcassi, bknerr
// * @since 26.11.2010
// * @param <T> the type for the support
// * CHECKSTYLE OFF: AbstractClassName
// *                 This class is accessed statically, hence the name should be short and descriptive!
// *
// */
//public abstract class TypeSupport<T> {
//    // CHECKSTYLE ON : AbstractClassName
//    
//    /**
//     * Internal class to improve readability.
//     * @author bknerr
//     * @since 20.01.2011
//     */
//    private static final class TypeSupportMap<T> extends ConcurrentHashMap<Class<T>, TypeSupport<T>> {
//        private static final long serialVersionUID = -4188540118549188362L;
//        public TypeSupportMap() { /* EMPTY */ }
//    }
//    
//    @SuppressWarnings("rawtypes")
//    private static final Map<Class<? extends TypeSupport>, TypeSupportMap> ALL_TYPE_SUPPORTS = 
//        new ConcurrentHashMap<Class<? extends TypeSupport>, TypeSupportMap>();
//    @SuppressWarnings("rawtypes")
//    private static final Map<Class<? extends TypeSupport>, TypeSupportMap> ALL_CALC_TYPE_SUPPORTS = 
//        new ConcurrentHashMap<Class<? extends TypeSupport>, TypeSupportMap>();
//    
//    
//    @SuppressWarnings("rawtypes")
//    private static 
//    void addTypeSupportFamilyIfNotExists(@Nonnull final Map<Class<? extends TypeSupport>, TypeSupportMap> map, 
//                                         @Nonnull final Class<? extends TypeSupport> typeSupportFamily) {
//        
//        TypeSupportMap<?> familyMap = (TypeSupportMap<?>) map.get(typeSupportFamily);
//        if (familyMap == null) {
//            TypeSupportMap<?> supportMap = new TypeSupportMap();
//            map.put(typeSupportFamily, supportMap);
//        }
//    }
//    
//    
//    @SuppressWarnings("unchecked")
//    public static <T>
//    void addTypeSupport(@Nonnull final Class<T> typeClass, 
//                        @Nonnull final TypeSupport<T> typeSupport) {
//        Class<? extends TypeSupport<T>> typeSupportFamily = typeSupport.getTypeSupportFamily();
//        
//        addTypeSupportFamilyIfNotExists(ALL_TYPE_SUPPORTS, typeSupportFamily);
//        addTypeSupportFamilyIfNotExists(ALL_CALC_TYPE_SUPPORTS, typeSupportFamily);
//        
//        ALL_TYPE_SUPPORTS.get(typeSupportFamily).put(typeClass, typeSupport);
//        ALL_CALC_TYPE_SUPPORTS.get(typeSupportFamily).remove(typeClass);
//    }
//
//    /**
//     * Retrieve support for the given type and if not found looks at the
//     * implemented interfaces.
//     * If not found for the interfaces, traverse the superclass hierarchy.
//     *
//     * @param <T> the type to retrieve support for
//     * @param typeClass the class of the type
//     * @return the support for the type or null
//     */
//    @SuppressWarnings("unchecked")
//    @CheckForNull
//    static <T> TypeSupport<T> recursiveTypeSupportFor(@Nonnull final Class<T> typeClass,
//                                                      @Nonnull final TypeSupportMap<?> supportMap) {
//        TypeSupport<T> support = (TypeSupport<T>) supportMap.get(typeClass);
//        if (support == null) {
//            for (@SuppressWarnings("rawtypes") final Class clazz : typeClass.getInterfaces()) {
//                support = recursiveTypeSupportFor(clazz, supportMap);
//                if (support != null) {
//                    return support;
//                }
//            }
//        }
//        return support;
//    }
//    
//    /**
//     * Calculates and caches the type support for a particular class, so that
//     * introspection does not occur at every call.
//     *
//     * @param typeClass the class of the type
//     * @return the support for the type or null
//     * @param <T> the type to retrieve support for
//     * @throws TypeSupportException
//     */
//    @SuppressWarnings("unchecked")
//    @Nonnull
//    protected static <T> TypeSupport<T> cachedTypeSupportFor(@SuppressWarnings("rawtypes") @Nonnull final Class<? extends TypeSupport> supportFamily,
//                                                             @Nonnull final Class<T> typeClass) throws TypeSupportException {
//        
//        TypeSupportMap<T> supportMap = ALL_TYPE_SUPPORTS.get(supportFamily);
//        TypeSupportMap<T> calcSupportMap = ALL_CALC_TYPE_SUPPORTS.get(supportFamily);
//        
//        if (supportMap == null || calcSupportMap == null) {
//            throw new TypeSupportException("No type support found for family " + supportFamily, null);
//        }
//        
//        TypeSupport<T> support = (TypeSupport<T>) calcSupportMap.get(typeClass);
//        if (support == null) {
//            support = recursiveTypeSupportFor(typeClass, supportMap);
//            if (support == null) {
//                support = recursiveClassSupportFor(typeClass, supportMap);
//            }
//            if (support == null) {
//                throw new TypeSupportException("No type support found for type " + typeClass, null);
//            }
//            calcSupportMap.put(typeClass, support);
//        }
//        return support;
//    }
//
//    @CheckForNull
//    private static <T> TypeSupport<T> recursiveClassSupportFor(@Nonnull final Class<T> typeClass,
//                                                               @Nonnull final TypeSupportMap<T> supportMap) {
//        
//        TypeSupport<T> support = null;
//        Class<? super T> superClass = typeClass.getSuperclass();
//        while (!superClass.equals(Object.class)) {
//            support = (TypeSupport<T>) supportMap.get(superClass);
//            if (support != null) {
//                break;
//            }
//            superClass = superClass.getSuperclass();
//        }
//        return support;
//    }
//    

//    
//    @Nonnull
//    public abstract Class<? extends TypeSupport<T>> getTypeSupportFamily();
//}