# Nginx edge load balancer for 2 gateway instances

This guide describes a low-cost edge routing pattern:

client -> nginx (single endpoint) -> gateway-1/gateway-2 -> internal lb://service

## Why this pattern

- Keeps one client endpoint while scaling gateway horizontally.
- Avoids ALB cost for small/medium traffic.
- Works with current stateless auth model (JWT + Redis blacklist).

## Nginx config file

- Template: `docs/nginx-gateway-lb.conf`
- Replace these values before production use:
  - `server_name api.ticketon.site`
  - upstream server IPs `10.0.10.21:8080`, `10.0.10.22:8080`

## Rollout steps

1. Prepare two gateway instances and verify `/actuator/health` is `UP` on each.
2. Install nginx on an edge host or run nginx container.
3. Place `docs/nginx-gateway-lb.conf` under nginx conf.d.
4. Validate config: `nginx -t`.
5. Reload: `nginx -s reload`.
6. Point DNS `api.ticketon.site` to nginx host.
7. Run load tests and check 5xx/p95 on gateway and nginx.

## Important limits

- A single nginx host can become a bottleneck or SPOF.
- If traffic grows, scale edge to two nginx hosts and put DNS failover or a managed LB in front.
- Keep gateway and broker scale-out independent; edge scale-out does not replace backend scale-out.

## Is Nginx lightweight?

Usually yes.

- Typical idle memory is small (often tens of MB for a basic setup).
- CPU usage is low under moderate request rates.
- Throughput is high for plain reverse proxy workloads.

But memory and CPU can increase quickly with:

- many concurrent keep-alive connections,
- large response buffers,
- TLS termination at high QPS,
- heavy logging.

For this project, nginx is a good low-cost start, but monitor and revisit when sustained polling load approaches gateway limits.
