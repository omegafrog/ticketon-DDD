package org.codenbug.event.application;

@FunctionalInterface
public interface Specification<T> {
    boolean isSatisfiedBy(T candidate);

    default Specification<T> and(Specification<T> other){
        return c->this.isSatisfiedBy(c) && other.isSatisfiedBy(c);
    }
    default Specification<T> or(Specification<T> other){
        return c->this.isSatisfiedBy(c) || other.isSatisfiedBy(c);
    }
}
