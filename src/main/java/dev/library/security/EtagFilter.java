package dev.library.security;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

// 2xx GET 응답 본문의 해시를 ETag로 붙이고 If-None-Match가 같으면 304로 본문을 생략한다.
// no-store 응답은 건너뛴다. ponytail: 응답은 매번 끝까지 만든다(전송량만 절약). DB 조회까지 줄이려면 엔드포인트별 버전 기반 ETag.
@Component
class EtagFilter extends ShallowEtagHeaderFilter {}
